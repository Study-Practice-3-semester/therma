import { useCallback, useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import * as api from './api';
import { FALLBACK_COWPERS, mockHistory, mockPoint, mockRecommendations } from './mock';
import type {
  ControlsBody,
  Cowper,
  Recommendation,
  SystemStatus,
  SystemStatusValue,
  TelemetryPoint,
} from './types';
import { normalizePoint } from './utils';

const MAX_POINTS = 900;
const HISTORY_MINUTES = 30;

export function useTherma() {
  const [cowpers, setCowpers] = useState<Cowper[]>(FALLBACK_COWPERS);
  const [selected, setSelected] = useState('ALL');
  const [history, setHistory] = useState<Record<string, TelemetryPoint[]>>(() => mockHistory(15));
  const [recommendations, setRecommendations] = useState<Recommendation[]>(() =>
    mockRecommendations(),
  );
  const [status, setStatus] = useState<SystemStatusValue>('ACTIVE');
  const [statusMessage, setStatusMessage] = useState<string | undefined>();
  const [demo, setDemo] = useState(true);
  const [connected, setConnected] = useState(false);
  const [simRunning, setSimRunning] = useState(true);
  const [lastAlert, setLastAlert] = useState<Recommendation | null>(null);

  const appendPoint = useCallback((raw: TelemetryPoint) => {
    if (!raw?.cowperId) return;
    const p = normalizePoint(raw);
    setHistory((prev) => {
      const arr = [...(prev[p.cowperId] ?? []), p];
      if (arr.length > MAX_POINTS) arr.splice(0, arr.length - MAX_POINTS);
      return { ...prev, [p.cowperId]: arr };
    });
  }, []);

  // Начальная загрузка: справочник, история, рекомендации, статус.
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const list = await api.getCowpers();
        if (cancelled || !Array.isArray(list) || !list.length) return;
        setCowpers(list);

        const to = new Date();
        const from = new Date(to.getTime() - HISTORY_MINUTES * 60_000);
        const results = await Promise.allSettled(
          list.map((c) => api.getTelemetry(c.id, from, to)),
        );
        if (cancelled) return;

        const hist: Record<string, TelemetryPoint[]> = {};
        let total = 0;
        results.forEach((res, i) => {
          if (res.status === 'fulfilled' && Array.isArray(res.value)) {
            hist[list[i].id] = res.value.map(normalizePoint).slice(-MAX_POINTS);
            total += res.value.length;
          }
        });
        if (total > 0) {
          setHistory(hist);
          setDemo(false);
        }
      } catch {
        /* бэкенд недоступен — работаем в демо-режиме */
      }

      try {
        const [recs, st] = await Promise.all([api.getRecommendations(), api.getSystemStatus()]);
        if (cancelled) return;
        if (Array.isArray(recs) && recs.length) setRecommendations(recs);
        if (st?.status) {
          setStatus(st.status);
          setStatusMessage(st.message);
        }
      } catch {
        /* остаёмся на демо-данных */
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  // STOMP: телеметрия, рекомендации, статус системы.
  useEffect(() => {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new Client({
      brokerURL: `${proto}://${window.location.host}/ws-endpoint`,
      reconnectDelay: 5000,
      debug: () => {},
    });

    client.onConnect = () => {
      setConnected(true);
      setDemo(false);
      client.subscribe('/topic/telemetry', (frame) => {
        try {
          appendPoint(JSON.parse(frame.body) as TelemetryPoint);
        } catch {
          /* ignore */
        }
      });
      client.subscribe('/topic/recommendations', (frame) => {
        try {
          const rec = JSON.parse(frame.body) as Recommendation;
          setRecommendations((prev) => {
            const exists = prev.some((r) => r.id === rec.id);
            return exists ? prev.map((r) => (r.id === rec.id ? rec : r)) : [rec, ...prev];
          });
          setLastAlert(rec);
        } catch {
          /* ignore */
        }
      });
      client.subscribe('/topic/system-status', (frame) => {
        try {
          const s = JSON.parse(frame.body) as SystemStatus;
          if (s?.status) {
            setStatus(s.status);
            setStatusMessage(s.message);
          }
        } catch {
          /* ignore */
        }
      });
    };
    client.onWebSocketClose = () => setConnected(false);
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [appendPoint]);

  // Генератор демо-телеметрии, пока нет реального потока.
  useEffect(() => {
    if (!demo || !simRunning) return;
    const tick = () => {
      const at = Date.now();
      cowpers.forEach((c, i) => {
        appendPoint({ ...mockPoint(i, at), cowperId: c.id });
      });
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [demo, simRunning, appendPoint, cowpers]);

  const applyRec = useCallback(
    async (rec: Recommendation) => {
      try {
        if (!demo) await api.applyRecommendation(rec.id, rec.suggestedValueMax);
      } catch (e) {
        console.warn('apply failed', e);
      }
      setRecommendations((prev) =>
        prev.map((r) => (r.id === rec.id ? { ...r, status: 'APPLIED' } : r)),
      );
    },
    [demo],
  );

  const rejectRec = useCallback(
    async (rec: Recommendation) => {
      if (!demo) await api.rejectRecommendation(rec.id);
      setRecommendations((prev) =>
        prev.map((r) => (r.id === rec.id ? { ...r, status: 'REJECTED' } : r)),
      );
    },
    [demo],
  );

  const startSim = useCallback(
    async (scenarioId: number) => {
      setSimRunning(true);
      if (demo) {
        if (scenarioId === 3) {
          setStatus('INSUFFICIENT_DATA');
          setStatusMessage(
            'Отсутствует телеметрия более 10 секунд. Выдача рекомендаций приостановлена.',
          );
        } else {
          setStatus('ACTIVE');
          setStatusMessage(undefined);
        }
      }
      try {
        if (!demo) await api.startSimulator(scenarioId);
      } catch {
        /* ignore */
      }
    },
    [demo],
  );

  const stopSim = useCallback(async () => {
    setSimRunning(false);
    try {
      if (!demo) await api.stopSimulator();
    } catch {
      /* ignore */
    }
  }, [demo]);

  const submitControls = useCallback(
    async (body: ControlsBody): Promise<{ ok: boolean; message: string }> => {
      if (!selected || selected === 'ALL') {
        return { ok: false, message: 'Выберите конкретный воздухонагреватель' };
      }
      try {
        if (!demo) await api.sendControls(selected, body);
        return { ok: true, message: demo ? 'Параметры применены (демо-режим)' : 'Параметры отправлены' };
      } catch {
        return { ok: false, message: 'Не удалось отправить параметры' };
      }
    },
    [demo, selected],
  );

  return {
    cowpers,
    selected,
    setSelected,
    history,
    recommendations,
    status,
    statusMessage,
    demo,
    connected,
    simRunning,
    lastAlert,
    clearLastAlert: () => setLastAlert(null),
    applyRec,
    rejectRec,
    startSim,
    stopSim,
    submitControls,
  };
}

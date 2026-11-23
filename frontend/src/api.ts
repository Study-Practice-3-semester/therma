import axios from 'axios';
import type { ControlsBody, Cowper, Recommendation, SystemStatus, TelemetryPoint } from './types';

export const api = axios.create({ baseURL: '/api/v1', timeout: 15000 });

export interface TickResponse {
  points: TelemetryPoint[];
  recommendations: Recommendation[];
}

export const getCowpers = () => api.get<Cowper[]>('/cowpers').then((r) => r.data);

/**
 * Бэкенд (CsvMockTelemetryController) отдаёт последние `limitMinutes` минут
 * с шагом ~15 секунд и игнорирует from/to — они отправляются на будущее.
 * 60 минут ≈ 240 точек на каупер, больше брать нельзя (тормозит отрисовка).
 */
export const getTelemetry = (cowperId: string, from: Date, to: Date, limitMinutes = 60) =>
  api
    .get<TelemetryPoint[]>('/telemetry', {
      params: {
        cowperId,
        from: from.toISOString(),
        to: to.toISOString(),
        limitMinutes,
      },
    })
    .then((r) => r.data);

/** Пока STOMP не прикручен: двигает симуляцию на 1 минуту и отдаёт новые точки + алерты. */
export const nextTick = () => api.post<TickResponse>('/telemetry/next-tick').then((r) => r.data);

export const getRecommendations = () =>
  api.get<Recommendation[]>('/recommendations').then((r) => r.data);

export const getSystemStatus = () => api.get<SystemStatus>('/system/status').then((r) => r.data);

export const applyRecommendation = (id: number, appliedValue?: number) =>
  api.post(`/recommendations/${id}/apply`, { appliedValue });

/** Отдельного эндпоинта в OpenAPI пока нет — пробуем и не падаем. */
export const rejectRecommendation = async (id: number) => {
  try {
    await api.post(`/recommendations/${id}/reject`);
  } catch {
    /* локальное отклонение */
  }
};

export const startSimulator = (scenarioId: number) =>
  api.post('/simulator/start', { scenarioId });

export const stopSimulator = () => api.post('/simulator/stop');

export const sendControls = (cowperId: string, body: ControlsBody) =>
  api.post(`/cowpers/${cowperId}/controls`, body);

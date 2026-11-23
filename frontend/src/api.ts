import axios from 'axios';
import type { ControlsBody, Cowper, Recommendation, SystemStatus, TelemetryPoint } from './types';

export const api = axios.create({ baseURL: '/api/v1', timeout: 5000 });

export const getCowpers = () => api.get<Cowper[]>('/cowpers').then((r) => r.data);

export const getTelemetry = (cowperId: string, from: Date, to: Date) =>
  api
    .get<TelemetryPoint[]>('/telemetry', {
      params: { cowperId, from: from.toISOString(), to: to.toISOString() },
    })
    .then((r) => r.data);

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

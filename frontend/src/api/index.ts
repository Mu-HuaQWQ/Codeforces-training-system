import type {
  ProgressInfo,
  RadarData,
  UserInput,
  UserStatsResponse,
  Student,
  StudentDetail,
} from '../types';

const BASE = '/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
  }
  return res.json();
}

export function startCrawl(users: UserInput[]): Promise<{ status: string }> {
  return request('/crawl', {
    method: 'POST',
    body: JSON.stringify({ users }),
  });
}

export function getProgress(): Promise<ProgressInfo> {
  return request('/crawl/progress');
}

export function getResults(): Promise<UserStatsResponse[]> {
  return request('/crawl/results');
}

export function getComparison(handles: string[]): Promise<RadarData[]> {
  return request('/stats/compare', {
    method: 'POST',
    body: JSON.stringify({ handles }),
  });
}

export function getStudents(): Promise<Student[]> {
  return request('/students');
}

export function addStudent(data: { name: string; handle: string; platform: string }): Promise<Student> {
  return request('/students', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function deleteStudent(id: number): Promise<void> {
  return request(`/students/${id}`, { method: 'DELETE' });
}

export function getStudentDetail(id: number): Promise<StudentDetail> {
  return request(`/students/${id}`);
}

export function refreshAll(): Promise<{ refreshed: number }> {
  return request('/crawl/refresh-all', { method: 'POST' });
}

export type Platform = 'CODEFORCES' | 'LUOGU';

export interface UserInput {
  handle: string;
  platform: Platform;
}

export interface ProgressInfo {
  completed: number;
  failed: number;
  total: number;
  done: boolean;
}

export interface UserProfile {
  handle: string;
  platform: string;
  rating: number | null;
  maxRating: number | null;
  rank: string | null;
  contribution: number | null;
}

export interface TagCount {
  [tag: string]: number;
}

export interface UserStatistics {
  handle: string;
  platform: string;
  totalSubmissions: number;
  acceptedCount: number;
  acceptanceRate: number;
  tagAcceptedCount: TagCount;
  tagTotalCount: TagCount;
  difficultyCount: TagCount;
  maxStreak: number;
}

export interface RadarData {
  handle: string;
  labels: string[];
  values: number[];
}

export interface UserStatsResponse {
  handle: string;
  platform: string;
  profile: UserProfile | null;
  stats: UserStatistics | null;
  failed: boolean;
  error: string | null;
}

export interface Student {
  id: number;
  name: string;
  handle: string;
  platform: string;
  currentRating: number | null;
  weeklyAcCount: number;
  createdAt: string;
}

export interface RatingRecord {
  id: number;
  rating: number;
  recordedAt: string;
}

export interface ContestRecord {
  id: number;
  contestName: string;
  rank: number | null;
  oldRating: number;
  newRating: number;
  ratingChange: number | null;
  solvedCount: number | null;
  contestDate: string;
}

export interface StudentDetail {
  id: number;
  name: string;
  handle: string;
  platform: string;
  profile: UserProfile | null;
  stats: UserStatistics | null;
  radarData: RadarData[];
  ratingHistory: RatingRecord[];
  contests: ContestRecord[];
  createdAt: string;
}

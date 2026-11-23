export interface JourneyTimelineItem {
  id: string;
  title: string;
  employer: string | null;
  startMonth: string | null;
  endMonth: string | null;
  isCurrent: boolean;
}

export interface JourneySkill {
  skillId: string;
  name: string;
  level: string | null;
  verified: boolean;
  firstUsedMonth: string | null;
  lastUsedMonth: string | null;
  roleTitles: string[];
}

export interface Journey {
  timeline: JourneyTimelineItem[];
  skills: JourneySkill[];
}

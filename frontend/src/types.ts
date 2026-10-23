export type User = {
  id: number;
  email: string;
};

export type Phrase = {
  id: number;
  englishText: string;
  japaneseText: string;
  exampleSentence: string | null;
  category: string | null;
  intervalStage: number;
  nextReviewDate: string;
  createdAt: string;
  updatedAt: string;
};

export type Dashboard = {
  dueTodayCount: number;
  totalPhraseCount: number;
  masteredCount: number;
  today: string;
  streak: {
    days: number;
    todayReviewed: boolean;
  };
  calendar: {
    year: number;
    month: number;
    reviewedDates: string[];
  };
};

export type PhraseInput = {
  englishText: string;
  japaneseText: string;
  exampleSentence: string;
  category: string;
};

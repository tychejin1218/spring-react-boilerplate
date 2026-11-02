import { z } from 'zod';

export interface Todo {
  id: number;
  title: string;
  completed: boolean;
  imageUrl?: string;
}

export interface CreateTodoRequest {
  title: string;
}

/** 할 일 추가 폼 스키마 (앞뒤 공백 제거 후 1자 이상) */
export const createTodoFormSchema = z.object({
  title: z.string().trim().min(1),
});
export type CreateTodoFormValues = z.infer<typeof createTodoFormSchema>;

export type TodoFilter = 'all' | 'completed' | 'incomplete';

export type TodoSort = 'id' | 'title';

export interface TodoListParams {
  page: number;
  size: number;
  status: TodoFilter;
  sort: TodoSort;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

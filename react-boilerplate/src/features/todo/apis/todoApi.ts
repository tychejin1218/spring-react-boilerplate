import api from '@/shared/apis/instance';
import type { ApiResponse } from '@/shared/types/api';
import type { Todo, CreateTodoRequest, TodoListParams, PageResponse } from '@/features/todo/types/todo';

export const getTodos = (params: TodoListParams): Promise<ApiResponse<PageResponse<Todo>>> =>
  api.get<ApiResponse<PageResponse<Todo>>>('/todos', { params }).then((res) => res.data);

export const postTodo = (data: CreateTodoRequest): Promise<ApiResponse<Todo>> => api.post<ApiResponse<Todo>>('/todos', data).then((res) => res.data);

export const patchTodoComplete = (id: number): Promise<ApiResponse<Todo>> => api.patch<ApiResponse<Todo>>(`/todos/${id}/complete`).then((res) => res.data);

export const patchTodoImage = (id: number, imageUrl: string): Promise<ApiResponse<Todo>> =>
  api.patch<ApiResponse<Todo>>(`/todos/${id}/image`, { imageUrl }).then((res) => res.data);

export const deleteTodo = (id: number): Promise<ApiResponse<null>> => api.delete<ApiResponse<null>>(`/todos/${id}`).then((res) => res.data);

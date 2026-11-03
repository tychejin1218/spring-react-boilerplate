import { useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { todoKeys } from '@/shared/apis/queryKeys';
import { deleteTodo, getTodos, patchTodoComplete, patchTodoImage, postTodo } from '@/features/todo/apis/todoApi';
import { getPresignedUrl, uploadToPresignedUrl } from '@/shared/apis/fileApi';
import type { CreateTodoRequest, TodoListParams } from '@/features/todo/types/todo';

export const useTodos = (params: TodoListParams) =>
  useQuery({
    queryKey: todoKeys.list(params).queryKey,
    queryFn: () => getTodos(params),
    select: (data) => data.data,
  });

export const useCreateTodo = (params: TodoListParams) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTodoRequest) => postTodo(data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: todoKeys.list(params).queryKey }),
  });
};

export const useCompleteTodo = (params: TodoListParams) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => patchTodoComplete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: todoKeys.list(params).queryKey }),
  });
};

export const useDeleteTodo = (params: TodoListParams) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteTodo(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: todoKeys.list(params).queryKey }),
  });
};

export const useUploadTodoImage = (params: TodoListParams) => {
  const queryClient = useQueryClient();
  const queryKey = todoKeys.list(params).queryKey;

  return useCallback(
    async (todoId: number, file: File) => {
      const { data } = await getPresignedUrl(file.name, file.type);
      await uploadToPresignedUrl(data.presignedUrl, file);
      await patchTodoImage(todoId, data.objectUrl);
      await queryClient.invalidateQueries({ queryKey });
    },
    [queryClient, queryKey],
  );
};

import { useState, useMemo, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTodos, useCreateTodo, useCompleteTodo, useDeleteTodo, useUploadTodoImage } from '@/features/todo/hooks/useTodos';
import TodoItem from '@/features/todo/components/TodoItem';
import { createTodoFormSchema, type CreateTodoFormValues, type TodoFilter, type TodoListParams, type TodoSort } from '@/features/todo/types/todo';
import Button from '@/shared/components/ui/Button';
import EmptyState from '@/shared/components/ui/EmptyState';
import Input from '@/shared/components/ui/Input';

const PAGE_SIZE = 10;

const FILTER_TABS: { label: string; value: TodoFilter }[] = [
  { label: '전체', value: 'all' },
  { label: '미완료', value: 'incomplete' },
  { label: '완료', value: 'completed' },
];

const TodoPage = () => {
  // 빈 제목(공백만 포함)은 에러 메시지 없이 제출만 막는다 (zod 스키마에서 trim 후 1자 이상 검사)
  const { register, handleSubmit, reset } = useForm<CreateTodoFormValues>({ resolver: zodResolver(createTodoFormSchema), defaultValues: { title: '' } });
  const [filter, setFilter] = useState<TodoFilter>('all');
  const [sort, setSort] = useState<TodoSort>('id');
  const [page, setPage] = useState(0);

  const params = useMemo<TodoListParams>(() => ({ page, size: PAGE_SIZE, status: filter, sort }), [page, filter, sort]);

  const { data: pageData, isLoading } = useTodos(params);
  const createTodo = useCreateTodo(params);
  const completeTodo = useCompleteTodo(params);
  const deleteTodo = useDeleteTodo(params);
  const uploadTodoImage = useUploadTodoImage(params);

  const todos = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  const onAdd = ({ title }: CreateTodoFormValues) => {
    createTodo.mutate({ title });
    reset();
  };

  const handleFilterChange = useCallback((newFilter: TodoFilter) => {
    setFilter(newFilter);
    setPage(0);
  }, []);

  const handleSortChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    setSort(e.target.value as TodoSort);
    setPage(0);
  }, []);

  return (
    <div className="mx-auto max-w-lg py-6">
      <h2 className="mb-4 text-xl font-bold text-gray-800">할 일 목록</h2>

      <form onSubmit={(e) => void handleSubmit(onAdd)(e)} className="mb-4 flex gap-2">
        <Input type="text" placeholder="새 할 일 입력" aria-label="새 할 일 입력" className="flex-1" {...register('title')} />
        <Button type="submit" disabled={createTodo.isPending}>
          추가
        </Button>
      </form>

      <div className="mb-4 flex items-center justify-between border-b border-gray-200">
        <div className="flex gap-1">
          {FILTER_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => handleFilterChange(tab.value)}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                filter === tab.value ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <select
          value={sort}
          onChange={handleSortChange}
          className="mb-1 rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-600 focus:outline-none"
        >
          <option value="id">최신순</option>
          <option value="title">제목순</option>
        </select>
      </div>

      {isLoading && <p className="text-sm text-gray-400">불러오는 중...</p>}

      {!isLoading && todos.length === 0 && <EmptyState message="할 일이 없습니다. 추가해보세요!" />}

      <ul className="flex flex-col gap-2">
        {todos.map((todo) => (
          <TodoItem
            key={todo.id}
            todo={todo}
            onComplete={(id) => completeTodo.mutate(id)}
            onDelete={(id) => deleteTodo.mutate(id)}
            onUpload={uploadTodoImage}
          />
        ))}
      </ul>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>
            이전
          </Button>
          <span className="text-sm text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <Button variant="secondary" size="sm" onClick={() => setPage((p) => p + 1)} disabled={pageData?.last ?? true}>
            다음
          </Button>
        </div>
      )}
    </div>
  );
};

export default TodoPage;

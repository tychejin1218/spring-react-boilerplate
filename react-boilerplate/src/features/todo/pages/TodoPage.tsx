import { useState, useMemo, useCallback } from 'react';
import { useTodos, useCreateTodo, useCompleteTodo, useDeleteTodo, useUploadTodoImage } from '@/features/todo/hooks/useTodos';
import TodoItem from '@/features/todo/components/TodoItem';
import type { TodoFilter, TodoListParams, TodoSort } from '@/features/todo/types/todo';

const PAGE_SIZE = 10;

const FILTER_TABS: { label: string; value: TodoFilter }[] = [
  { label: '전체', value: 'all' },
  { label: '미완료', value: 'incomplete' },
  { label: '완료', value: 'completed' },
];

const TodoPage = () => {
  const [input, setInput] = useState('');
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

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const title = input.trim();
    if (!title) return;
    createTodo.mutate({ title });
    setInput('');
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

      <form onSubmit={handleAdd} className="mb-4 flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="새 할 일 입력"
          className="flex-1 rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
        />
        <button
          type="submit"
          disabled={createTodo.isPending}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          추가
        </button>
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
        <select value={sort} onChange={handleSortChange} className="mb-1 rounded border border-gray-200 px-2 py-1 text-xs text-gray-600 focus:outline-none">
          <option value="id">최신순</option>
          <option value="title">제목순</option>
        </select>
      </div>

      {isLoading && <p className="text-sm text-gray-400">불러오는 중...</p>}

      {!isLoading && todos.length === 0 && <p className="text-sm text-gray-400">할 일이 없습니다. 추가해보세요!</p>}

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
          <button
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
            className="rounded border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-sm text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={pageData?.last ?? true}
            className="rounded border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
};

export default TodoPage;

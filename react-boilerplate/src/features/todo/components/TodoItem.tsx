import { useRef, useState } from 'react';
import type { Todo } from '@/features/todo/types/todo';

interface Props {
  todo: Todo;
  onComplete: (id: number) => void;
  onDelete: (id: number) => void;
  onUpload: (id: number, file: File) => Promise<void>;
}

const TodoItem = ({ todo, onComplete, onDelete, onUpload }: Props) => {
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      await onUpload(todo.id, file);
    } catch {
      console.error('[TodoItem] 이미지 업로드 실패');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <li className="flex items-center gap-3 rounded border border-gray-200 bg-white px-4 py-3">
      <input
        type="checkbox"
        checked={todo.completed}
        onChange={() => !todo.completed && onComplete(todo.id)}
        disabled={todo.completed}
        className="size-4 cursor-pointer accent-blue-600 disabled:cursor-default"
      />
      {todo.imageUrl && <img src={todo.imageUrl} alt="첨부 이미지" className="size-10 rounded object-cover" />}
      <span className={`flex-1 text-sm ${todo.completed ? 'text-gray-400 line-through' : 'text-gray-800'}`}>{todo.title}</span>
      <input ref={fileInputRef} type="file" accept="image/*" onChange={(e) => void handleFileChange(e)} className="hidden" />
      <button onClick={() => fileInputRef.current?.click()} disabled={uploading} className="text-xs text-gray-400 hover:text-blue-500 disabled:opacity-40">
        {uploading ? '...' : '사진'}
      </button>
      <button onClick={() => onDelete(todo.id)} className="text-xs text-red-400 hover:text-red-600">
        삭제
      </button>
    </li>
  );
};

export default TodoItem;

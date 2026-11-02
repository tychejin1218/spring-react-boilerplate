interface Props {
  /** sm: 버튼 안 인라인용, md: 기본 */
  size?: 'sm' | 'md';
  /** true 면 화면 전체 가운데에 배치 (Suspense fallback 용, 기본값) */
  fullScreen?: boolean;
}

const SIZE_MAP: Record<NonNullable<Props['size']>, string> = {
  sm: 'size-4 border-2',
  md: 'size-8 border-4',
};

/**
 * 로딩 스피너
 * - 기본값은 Suspense fallback 용 전체 화면 스피너
 * - `fullScreen={false}` 로 버튼 등 인라인 위치에 사용한다 (보조기기에는 숨김)
 */
const LoadingSpinner = ({ size = 'md', fullScreen = true }: Props) => {
  const spinner = <span aria-hidden="true" className={`${SIZE_MAP[size]} inline-block animate-spin rounded-full border-gray-200 border-t-gray-600`} />;

  if (!fullScreen) return spinner;

  return <div className="flex h-screen items-center justify-center">{spinner}</div>;
};

export default LoadingSpinner;

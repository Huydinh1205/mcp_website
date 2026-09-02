// Tiny inline-SVG icon set (no dependency). <Icon name="cart" />
// Stroke inherits currentColor; size via `size` prop.

type Name =
  | "cart"
  | "bolt"
  | "handshake"
  | "truck"
  | "package"
  | "check"
  | "search"
  | "user"
  | "star"
  | "heart"
  | "shield"
  | "tag"
  | "chevron"
  | "spark"
  | "trash";

const PATHS: Record<Name, React.ReactNode> = {
  cart: (
    <>
      <circle cx="9" cy="20" r="1.6" />
      <circle cx="18" cy="20" r="1.6" />
      <path d="M2.5 3h2.2l2.3 12.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21.5 7H6" />
    </>
  ),
  bolt: <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" />,
  handshake: (
    <path d="m11 17 2 2a2 2 0 0 0 2.9 0l4.1-4.1M8 12 6 10 3 13l5 5m3-1-3-3M7 8l3-3a2 2 0 0 1 2.5-.3l3 2 3-1 3 3" />
  ),
  truck: (
    <>
      <path d="M2 6h11v11H2zM13 9h5l3 3v5h-8" />
      <circle cx="6.5" cy="18.5" r="1.7" />
      <circle cx="17.5" cy="18.5" r="1.7" />
    </>
  ),
  package: (
    <path d="M21 8 12 3 3 8v8l9 5 9-5V8ZM3 8l9 5m0 0 9-5m-9 5v9" />
  ),
  check: <path d="m5 13 4 4L19 7" />,
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </>
  ),
  star: <path d="m12 3 2.9 5.9 6.5.9-4.7 4.6 1.1 6.5L12 17.8 6.2 21l1.1-6.5L2.6 9.8l6.5-.9L12 3Z" />,
  heart: <path d="M12 21s-7-4.4-9.5-9A5 5 0 0 1 12 6a5 5 0 0 1 9.5 6c-2.5 4.6-9.5 9-9.5 9Z" />,
  shield: <path d="M12 3 5 6v6c0 5 3.4 8 7 9 3.6-1 7-4 7-9V6l-7-3Z" />,
  tag: (
    <>
      <path d="M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9-9-9Z" />
      <circle cx="7.5" cy="7.5" r="1.3" />
    </>
  ),
  chevron: <path d="m9 6 6 6-6 6" />,
  spark: <path d="M12 2v6m0 8v6M2 12h6m8 0h6M5 5l4 4m6 6 4 4m0-14-4 4m-6 6-4 4" />,
  trash: <path d="M4 7h16M9 7V4h6v3m-8 0 1 14h8l1-14" />,
};

export function Icon({
  name,
  size = 18,
  className,
  fill = false,
}: {
  name: Name;
  size?: number;
  className?: string;
  fill?: boolean;
}) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={fill ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={1.7}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {PATHS[name]}
    </svg>
  );
}

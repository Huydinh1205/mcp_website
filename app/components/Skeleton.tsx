export function Skeleton({
  w,
  h = 14,
  r = 6,
  style,
}: {
  w?: number | string;
  h?: number | string;
  r?: number;
  style?: React.CSSProperties;
}) {
  return (
    <span
      className="skel"
      style={{ width: w ?? "100%", height: h, borderRadius: r, ...style }}
    />
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="pcard pcard--skel">
      <div className="pcard__media">
        <Skeleton h="100%" r={0} />
      </div>
      <div className="pcard__body">
        <Skeleton h={13} />
        <Skeleton h={13} w="70%" style={{ marginTop: 6 }} />
        <Skeleton h={16} w="40%" style={{ marginTop: 10 }} />
        <Skeleton h={11} w="55%" style={{ marginTop: 8 }} />
      </div>
    </div>
  );
}

export function ProductGridSkeleton({ n = 10 }: { n?: number }) {
  return (
    <div className="pgrid">
      {Array.from({ length: n }, (_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
}

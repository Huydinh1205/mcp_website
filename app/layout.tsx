import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Agent Negotiation Marketplace",
  description: "Buyers and sellers negotiate through their AI agents via WebMCP.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <nav className="topnav">
          <a href="/">Buyer</a>
          <a href="/dashboard">Seller dashboard</a>
          <span className="hint">Chrome 146+ recommended</span>
        </nav>
        {children}
      </body>
    </html>
  );
}

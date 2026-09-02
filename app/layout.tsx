import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Nav } from "@/app/components/Nav";
import { Toaster } from "@/app/components/Toaster";

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "Agent Negotiation Marketplace",
  description: "Buyers and sellers negotiate through their AI agents via WebMCP.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={inter.className}>
      <body>
        <Nav />
        {children}
        <Toaster />
      </body>
    </html>
  );
}

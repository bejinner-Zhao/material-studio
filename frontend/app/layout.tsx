import type { Metadata } from "next";

import { SiteHeader } from "@/components/SiteHeader";

import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "Material Studio · 素材制作",
  description: "浏览模板、上传图片、提交制作任务并查看结果",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-ink text-white antialiased">
        <Providers>
          <SiteHeader />
          <main className="mx-auto w-full max-w-6xl px-4 pb-16 pt-4">{children}</main>
        </Providers>
      </body>
    </html>
  );
}

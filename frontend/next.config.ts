import type { NextConfig } from "next";

// 浏览器直接请求 Java 后端：由 Next 的 rewrites 转发，避免跨域与额外中间层
const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${backendOrigin}/api/:path*` },
      { source: "/files/:path*", destination: `${backendOrigin}/files/:path*` },
      { source: "/demo/:path*", destination: `${backendOrigin}/demo/:path*` },
    ];
  },
};

export default nextConfig;

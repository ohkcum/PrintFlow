/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  experimental: {
    turbo: {},
  },
  transpilePackages: ["@printflow/common", "@printflow/api-client", "@printflow/auth"],
};

export default nextConfig;

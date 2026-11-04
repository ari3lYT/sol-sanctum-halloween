import { defineConfig } from 'vite';
export default defineConfig({esbuild:{jsx:'automatic'},base:'/halloween/',server:{port:5186,strictPort:true,proxy:{'/halloween/api':{target:'http://127.0.0.1:18126'}}}});

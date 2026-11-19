module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  productionSourceMap: false,
  lintOnSave: false,
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: process.env.VUE_APP_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/upload': {
        target: process.env.VUE_APP_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  configureWebpack: {
    performance: { hints: false }
  }
}

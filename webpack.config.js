const path = require('path');

module.exports = (_, { mode }) => {
  return {
    devtool: 'source-map',
    mode: mode,
    entry: mode === 'production' ? './frontend/index.js' : './frontend/client.js',
    output: {
      path: path.resolve('./backend/www'),
      filename: 'svarog-notifications.js',
      library: 'svarog-notifications',
      libraryTarget: 'umd',
      globalObject: 'this'
    },
    devServer: {
      client: {
        overlay: false
      },
      static: {
        directory: path.join(__dirname, './backend/www'),
      },
      compress: true,
    },
    module: {
      rules: [
        {
          test: /\.(js|jsx)?$/,
          exclude: /(node_modules)/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env', '@babel/preset-react'],
              cacheDirectory: true
            }
          }
        },
        {
          test: /(\.jsx|\.js)$/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env', '@babel/preset-react'],
              cacheDirectory: true
            }
          },
          enforce: 'pre',
          include: [/perun-core/]
        },
        {
          // For pure CSS (without CSS modules)
          test: /\.css$/i,
          exclude: /\.module\.css$/i,
          use: ['style-loader', 'css-loader'],
        },
        {
          // For CSS modules
          test: /\.module\.css$/i,
          use: [
            'style-loader',
            {
              loader: 'css-loader',
              options: {
                sourceMap: true,
                modules: true,
                modules: {
                  localIdentName: '[name]-[local]'
                }
              },
            },
          ],
        },
        {
          test: /\.(png|jpe?g|gif|svg|eot|ttf|woff|woff2)$/i,
          type: 'asset/resource',
        },
      ]
    },
    resolve: {
      extensions: ['.js', '.jsx']
    },
    externals: mode === 'production' ? { 'perun-core': 'perun-core' } : {}
  }
};

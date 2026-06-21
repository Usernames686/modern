import Layout from '@/layout';

const orderRouter = {
  path: '/statistic',
  component: Layout,
  redirect: '/statistic/product',
  name: 'statistic',
  alwaysShow: true,
  meta: {
    title: '统计管理',
    icon: 'clipboard',
  },
  children: [
    {
      path: 'product',
      name: `product`,
      meta: { title: '商品统计' },
      component: () => import('@/views/statistic/product/index'),
      // children: [
      //   {
      //     path: 'visualization',
      //     component: () => import('@/views/statistic/product/visualization'),
      //     name: 'visualization',
      //     meta: { title: '统计', icon: '' }
      //   },
      //   {
      //     path: 'tableData',
      //     component: () => import('@/views/statistic/product/tableData'),
      //     name: 'tableData',
      //     meta: { title: '商品排行', icon: '' }
      //   },
      // ]
    },
    {
      path: 'statuser',
      name: `statuser`,
      meta: { title: '用户统计' },
      component: () => import('@/views/statistic/user/index'),
    },
    {
      path: 'transaction',
      name: `transaction`,
      meta: { title: '交易统计' },
      component: () => import('@/views/statistic/transaction/index'),
    },
  ],
};

export default orderRouter;

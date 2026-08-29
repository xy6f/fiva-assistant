<template>
  <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:60vh">
    <h1 style="font-size:72px;color:#909399;margin:0">404</h1>
    <p style="font-size:18px;color:#606266;margin:12px 0">页面不存在</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
  </div>
</template>

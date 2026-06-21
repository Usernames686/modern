<template>
  <div class="divBox page-diy-manager">
    <el-card class="box-card" shadow="never">
      <div class="diy-shell">
        <aside class="tab-view">
          <button :class="{ active: activeTab === 'home' }" type="button" @click="activeTab = 'home'">商城首页</button>
          <button :class="{ active: activeTab === 'pages' }" type="button" @click="activeTab = 'pages'">自定义页面</button>
        </aside>

        <section v-if="activeTab === 'home'" class="home-panel">
          <div class="phone-preview">
            <iframe v-if="frontDomain" :src="frontDomain" title="商城首页预览" />
            <div class="preview-mask"></div>
          </div>
          <div class="home-actions">
            <el-button type="primary" size="small" @click="editHome">首页装修</el-button>
            <el-card class="qr-card" shadow="never">
              <div>
                <div class="qr-title">微信小程序</div>
                <div class="qr-tip">未配置小程序码</div>
              </div>
              <div class="qr-placeholder">MP</div>
            </el-card>
            <el-card class="qr-card" shadow="never">
              <div>
                <div class="qr-title">微信公众号</div>
                <div class="qr-tip">{{ frontDomain || '未配置用户端地址' }}</div>
              </div>
              <div class="qr-placeholder">H5</div>
            </el-card>
          </div>
        </section>

        <section v-else class="page-panel">
          <div class="toolbar">
            <el-form inline size="small" :model="query" @submit.prevent>
              <el-form-item label="模板名称：">
                <el-input v-model.trim="query.name" class="selWidth" clearable placeholder="请输入模板名称" @keyup.enter="search" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
              </el-form-item>
            </el-form>
            <div class="toolbar-actions">
              <el-button type="primary" size="small" @click="createPage">添加</el-button>
              <el-button size="small" @click="loadList">刷新</el-button>
            </div>
          </div>

          <el-table v-loading="loading" :data="rows" size="small" class="table" highlight-current-row>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="模板名称" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="name-cell">
                  <el-tag v-if="Number(row.isDefault) === 1" effect="plain" size="small">首页</el-tag>
                  <span>{{ row.name || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="页面标题" min-width="120" />
            <el-table-column label="封面" width="90">
              <template #default="{ row }">
                <el-image
                  v-if="row.coverImage"
                  class="cover-image"
                  :src="assetUrl(row.coverImage)"
                  fit="cover"
                  :preview-src-list="[assetUrl(row.coverImage)]"
                  preview-teleported
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="添加时间" min-width="170">
              <template #default="{ row }">{{ dateText(row.addTime) }}</template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">{{ dateText(row.updateTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDesigner(row.id)">设计</el-button>
                <el-divider direction="vertical" />
                <el-button v-if="Number(row.isDefault) !== 1" link type="primary" @click="setHomepage(row.id)">设为首页</el-button>
                <el-divider v-if="Number(row.isDefault) !== 1" direction="vertical" />
                <el-dropdown trigger="click">
                  <el-button link type="primary">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="editPage(row.id)">基础信息</el-dropdown-item>
                      <el-dropdown-item @click="copyPage(row.id)">复制</el-dropdown-item>
                      <el-dropdown-item @click="previewPage(row.id)">预览</el-dropdown-item>
                      <el-dropdown-item v-if="Number(row.isDefault) !== 1" divided @click="deletePage(row.id)">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
          <div class="block">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.limit"
              :page-sizes="[10, 20, 30]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              background
              @size-change="loadList"
              @current-change="loadList"
            />
          </div>
        </section>
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" size="520px" :title="drawerTitle" @closed="resetForm">
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="模板名称：">
          <el-input v-model.trim="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="页面标题：">
          <el-input v-model.trim="form.title" />
        </el-form-item>
        <el-form-item label="封面图：">
          <div class="image-field">
            <el-image v-if="form.coverImage" class="form-image-preview" :src="assetUrl(form.coverImage)" fit="cover" />
            <span v-else class="form-image-empty">无图</span>
            <el-input v-model.trim="form.coverImage" placeholder="图片地址" />
            <el-button @click="openAttachmentSelector('coverImage')">选择</el-button>
            <el-button v-if="form.coverImage" @click="form.coverImage = ''">清除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="模板标识：">
          <el-input v-model.trim="form.templateName" />
        </el-form-item>
        <el-form-item label="显示首页：">
          <el-radio-group v-model="form.isShow">
            <el-radio-button :label="1">显示</el-radio-button>
            <el-radio-button :label="0">隐藏</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="背景颜色：">
          <el-input v-model.trim="form.colorPicker" />
        </el-form-item>
        <el-form-item label="背景图：">
          <div class="image-field">
            <el-image v-if="form.bgPic" class="form-image-preview" :src="assetUrl(form.bgPic)" fit="cover" />
            <span v-else class="form-image-empty">无图</span>
            <el-input v-model.trim="form.bgPic" placeholder="图片地址" />
            <el-button @click="openAttachmentSelector('bgPic')">选择</el-button>
            <el-button v-if="form.bgPic" @click="form.bgPic = ''">清除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="标题背景：">
          <el-input v-model.trim="form.titleBgColor" />
        </el-form-item>
        <el-form-item label="标题颜色：">
          <el-input v-model.trim="form.titleColor" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePage">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer
      v-model="designerVisible"
      size="100%"
      class="page-diy-designer-drawer"
      destroy-on-close
      :with-header="false"
      @closed="resetDesigner"
    >
      <div class="designer-head">
        <div>
          <strong>当前页面：{{ designerForm.name || '-' }}</strong>
          <span>ID：{{ designerForm.id || '新增' }}</span>
        </div>
        <div class="designer-actions">
          <el-button size="small" @click="editPage(designerForm.id)">基础信息</el-button>
          <el-button size="small" @click="previewPage(designerForm.id)">预览</el-button>
          <el-button size="small" :loading="designerSaving" @click="saveDesigner(false)">仅保存</el-button>
          <el-button type="primary" size="small" :loading="designerSaving" @click="saveDesigner(true)">保存关闭</el-button>
        </div>
      </div>

      <div class="designer-layout">
        <aside class="designer-palette">
          <div v-for="group in componentGroups" :key="group.title" class="palette-group">
            <div class="palette-title">{{ group.title }}</div>
            <button
              v-for="component in group.list"
              :key="component.type"
              type="button"
              class="palette-item"
              draggable="true"
              @dragstart="dragComponent(component)"
              @click="addComponent(component)"
            >
              <span>{{ component.icon }}</span>
              <em>{{ component.label }}</em>
            </button>
          </div>
        </aside>

        <main class="designer-phone-wrap">
          <div class="designer-phone">
            <div class="phone-status"></div>
            <div
              class="phone-title"
              :style="{ background: titleBgColor(designerForm.titleBgColor), color: titleColor(designerForm.titleColor) }"
              :class="{ active: selectedIndex === -1 }"
              @click="selectPageSetting"
            >
              {{ designerForm.title || '首页' }}
            </div>
            <div
              class="phone-body"
              :style="{
                backgroundColor: designerForm.isBgColor ? (designerForm.colorPicker || '#f5f5f5') : '#f5f5f5',
                backgroundImage: designerForm.isBgPic && designerForm.bgPic ? `url(${assetUrl(designerForm.bgPic)})` : ''
              }"
              @dragover.prevent
              @drop="dropComponentAt(designerComponents.length)"
            >
              <div v-if="!designerComponents.length" class="phone-empty">从左侧添加组件</div>
              <article
                v-for="(component, index) in designerComponents"
                :key="component.uid"
                class="phone-component"
                :class="{ active: selectedIndex === index, hidden: component.isHide }"
                draggable="true"
                @dragstart="dragExisting(index)"
                @dragover.prevent
                @drop.stop="dropComponentAt(index)"
                @click.stop="selectComponent(index)"
              >
                <div class="component-toolbar">
                  <button type="button" :disabled="index === 0" @click.stop="moveComponent(index, -1)">↑</button>
                  <button type="button" :disabled="index === designerComponents.length - 1" @click.stop="moveComponent(index, 1)">↓</button>
                  <button type="button" @click.stop="copyComponent(index)">复制</button>
                  <button type="button" @click.stop="toggleComponent(index)">{{ component.isHide ? '显示' : '隐藏' }}</button>
                  <button type="button" @click.stop="removeComponent(index)">删除</button>
                </div>
                <span class="component-name">{{ component.cname || component.label }}</span>
                <component-preview :component="component" :asset-url="assetUrl" />
              </article>
            </div>
          </div>
        </main>

        <aside class="designer-config">
          <template v-if="selectedIndex === -1">
            <h3>页面设置</h3>
            <el-form label-width="96px" size="small">
              <el-form-item label="页面名称">
                <el-input v-model.trim="designerForm.name" maxlength="50" />
              </el-form-item>
              <el-form-item label="页面标题">
                <el-input v-model.trim="designerForm.title" maxlength="30" />
              </el-form-item>
              <el-form-item label="标题背景">
                <el-input v-model.trim="designerForm.titleBgColor" placeholder="1 或 #ffffff" />
              </el-form-item>
              <el-form-item label="标题颜色">
                <el-input v-model.trim="designerForm.titleColor" placeholder="1 或 #333333" />
              </el-form-item>
              <el-form-item label="页面背景">
                <el-switch v-model="designerForm.isBgColor" :active-value="1" :inactive-value="0" />
                <el-input v-model.trim="designerForm.colorPicker" class="config-inline-input" placeholder="#f5f5f5" />
              </el-form-item>
              <el-form-item label="背景图片">
                <div class="image-field config-image-field">
                  <el-image v-if="designerForm.bgPic" class="form-image-preview" :src="assetUrl(designerForm.bgPic)" fit="cover" />
                  <span v-else class="form-image-empty">无图</span>
                  <el-input v-model.trim="designerForm.bgPic" placeholder="图片地址" />
                  <el-button @click="openAttachmentSelector('bgPic')">选择</el-button>
                </div>
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeComponent">
            <h3>{{ activeComponent.cname || activeComponent.label }}</h3>
            <el-form label-width="92px" size="small">
              <el-form-item label="组件标题">
                <el-input v-model.trim="activeComponent.title" maxlength="40" />
              </el-form-item>
              <el-form-item label="是否隐藏">
                <el-switch v-model="activeComponent.isHide" />
              </el-form-item>
              <template v-if="supportsSearchBox(activeComponent)">
                <el-form-item label="默认 Logo">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.logo" class="form-image-preview" :src="assetUrl(activeComponent.logo)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.logo" placeholder="logo 图片地址" />
                    <el-button @click="selectSearchLogo(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="提示文字">
                  <el-input v-model.trim="activeComponent.hotWord" maxlength="30" placeholder="搜索商品名称" />
                </el-form-item>
                <el-form-item label="热词">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.hotWords" :key="item.uid" class="config-row">
                      <el-input v-model.trim="item.title" placeholder="热词" />
                      <el-button @click="removeHotWord(activeComponent, index)">删除</el-button>
                    </div>
                    <el-button type="primary" plain :disabled="activeComponent.hotWords.length >= 20" @click="addHotWord(activeComponent)">添加热词</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsHomeComb(activeComponent)">
                <el-form-item label="搜索 Logo">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.logo" class="form-image-preview" :src="assetUrl(activeComponent.logo)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.logo" placeholder="logo 图片地址" />
                    <el-button @click="selectHomeCombLogo(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="搜索提示">
                  <el-input v-model.trim="activeComponent.hotWord" maxlength="30" placeholder="搜索商品名称" />
                </el-form-item>
              </template>
              <template v-if="supportsNewsRoll(activeComponent)">
                <el-form-item label="公告图标">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.logo" class="form-image-preview" :src="assetUrl(activeComponent.logo)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.logo" placeholder="公告图标地址" />
                    <el-button @click="selectNewsLogo(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="公告列表">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.notices" :key="item.uid" class="config-row">
                      <el-input v-model.trim="item.title" placeholder="公告标题" />
                      <el-input v-model.trim="item.link" placeholder="跳转链接" />
                      <el-button @click="openLinkSelector((link) => { item.link = link.url; item.title = item.title || link.name; })">选链接</el-button>
                      <el-button @click="removeNotice(activeComponent, index)">删除</el-button>
                    </div>
                    <el-button type="primary" plain @click="addNotice(activeComponent)">添加公告</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsTitle(activeComponent)">
                <el-form-item label="主标题">
                  <el-input v-model.trim="activeComponent.title" maxlength="16" placeholder="请输入主标题" />
                </el-form-item>
                <el-form-item label="副标题">
                  <el-input v-model.trim="activeComponent.subTitle" maxlength="24" placeholder="请输入副标题" />
                </el-form-item>
                <el-form-item label="右侧按钮">
                  <el-switch v-model="activeComponent.showMore" />
                </el-form-item>
                <el-form-item v-if="activeComponent.showMore" label="右侧文字">
                  <el-input v-model.trim="activeComponent.rightText" maxlength="8" placeholder="更多" />
                </el-form-item>
                <el-form-item label="背景图">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.bgImage" class="form-image-preview" :src="assetUrl(activeComponent.bgImage)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.bgImage" placeholder="背景图地址" />
                    <el-button @click="selectTitleBg(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsHomeTab(activeComponent)">
                <el-form-item label="选项卡">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.menus" :key="item.uid" class="config-row">
                      <el-input v-model.trim="item.title" placeholder="选项卡名称" />
                      <el-input v-model.trim="item.link" placeholder="跳转链接" />
                      <el-button @click="openLinkSelector((link) => { item.link = link.url; item.title = item.title || link.name; })">选链接</el-button>
                      <el-button @click="removeMenu(activeComponent, index)">删除</el-button>
                    </div>
                    <el-button type="primary" plain @click="addMenu(activeComponent)">添加选项卡</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsMerchants(activeComponent)">
                <el-form-item label="好店图标">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.logo" class="form-image-preview" :src="assetUrl(activeComponent.logo)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.logo" placeholder="图标地址" />
                    <el-button @click="selectMerchantLogo(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="商户列表">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.merchants" :key="item.uid" class="config-row merchant-config-row">
                      <el-image v-if="item.image" class="config-thumb" :src="assetUrl(item.image)" fit="cover" />
                      <span v-else class="config-thumb config-thumb-empty">店</span>
                      <el-input v-model.trim="item.title" placeholder="商户名称" />
                      <el-button @click="selectMerchantImage(activeComponent, index)">选图</el-button>
                      <el-button @click="removeMerchant(activeComponent, index)">删除</el-button>
                      <el-input v-model.trim="item.subTitle" placeholder="副标题/标签" />
                      <el-input v-model.trim="item.link" placeholder="跳转链接" />
                      <el-button @click="openLinkSelector((link) => { item.link = link.url; item.title = item.title || link.name; })">选链接</el-button>
                    </div>
                    <el-button type="primary" plain @click="addMerchant(activeComponent)">添加商户</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsImages(activeComponent)">
                <el-form-item label="图片">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.images" :key="item.uid" class="config-row">
                      <el-image v-if="item.image" class="config-thumb" :src="assetUrl(item.image)" fit="cover" />
                      <el-input v-model.trim="item.image" placeholder="图片地址" />
                      <el-button @click="selectImageFor(activeComponent, index)">选择</el-button>
                      <el-button @click="removeImage(activeComponent, index)">删除</el-button>
                      <el-input v-model.trim="item.title" placeholder="标题" />
                      <el-input v-model.trim="item.link" placeholder="链接" />
                      <el-button @click="openLinkSelector((link) => { item.link = link.url; item.title = item.title || link.name; })">选链接</el-button>
                    </div>
                    <el-button type="primary" plain @click="addImage(activeComponent)">添加图片</el-button>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsHotspots(activeComponent)">
                <el-form-item label="热区底图">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.image" class="form-image-preview" :src="assetUrl(activeComponent.image)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.image" placeholder="图片地址" />
                    <el-button @click="selectHotspotImage(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="点击热区">
                  <div class="hotspot-editor">
                    <div class="hotspot-preview">
                      <img v-if="activeComponent.image" :src="assetUrl(activeComponent.image)" alt="热区底图" />
                      <div v-else class="hotspot-empty">请先选择底图</div>
                      <button
                        v-for="(area, index) in activeComponent.hotspots"
                        :key="area.uid"
                        type="button"
                        class="hotspot-area"
                        :class="{ active: index === activeHotspotIndex }"
                        :style="hotspotStyle(area)"
                        @click.prevent="activeHotspotIndex = index"
                      >
                        {{ index + 1 }}
                      </button>
                    </div>
                    <div class="hotspot-list">
                      <div v-for="(area, index) in activeComponent.hotspots" :key="area.uid" class="hotspot-row">
                        <strong>热区 {{ index + 1 }}</strong>
                        <el-input v-model.trim="area.title" placeholder="名称" />
                        <el-input v-model.trim="area.link" placeholder="跳转链接" />
                        <el-button @click="openLinkSelector((link) => { area.link = link.url; area.title = area.title || link.name; })">选链接</el-button>
                        <el-input-number v-model="area.x" :min="0" :max="100" :precision="1" controls-position="right" />
                        <el-input-number v-model="area.y" :min="0" :max="100" :precision="1" controls-position="right" />
                        <el-input-number v-model="area.width" :min="1" :max="100" :precision="1" controls-position="right" />
                        <el-input-number v-model="area.height" :min="1" :max="100" :precision="1" controls-position="right" />
                        <el-button @click="removeHotspot(activeComponent, index)">删除</el-button>
                      </div>
                      <el-button type="primary" plain @click="addHotspot(activeComponent)">添加热区</el-button>
                    </div>
                  </div>
                </el-form-item>
              </template>
              <template v-if="supportsMenus(activeComponent)">
                <el-form-item :label="supportsFooter(activeComponent) ? '底部导航' : '菜单'">
                  <div class="config-list">
                    <div v-for="(item, index) in activeComponent.menus" :key="item.uid" class="config-row" :class="{ 'footer-config-row': supportsFooter(activeComponent) }">
                      <el-input v-model.trim="item.title" placeholder="菜单名称" />
                      <template v-if="supportsFooter(activeComponent)">
                        <el-input v-model.trim="item.activeImage" placeholder="选中图标地址" />
                        <el-button @click="selectFooterImage(activeComponent, index, 'activeImage')">选中图</el-button>
                        <el-input v-model.trim="item.inactiveImage" placeholder="未选中图标地址" />
                        <el-button @click="selectFooterImage(activeComponent, index, 'inactiveImage')">未选中图</el-button>
                      </template>
                      <template v-else>
                        <el-input v-model.trim="item.image" placeholder="图标地址" />
                        <el-button @click="selectMenuImage(activeComponent, index)">选图</el-button>
                      </template>
                      <el-input v-model.trim="item.link" placeholder="跳转链接" />
                      <el-button @click="openLinkSelector((link) => { item.link = link.url; item.title = item.title || link.name; })">选链接</el-button>
                      <el-button @click="removeMenu(activeComponent, index)">删除</el-button>
                    </div>
                    <el-button type="primary" plain :disabled="supportsFooter(activeComponent) && activeComponent.menus.length >= 5" @click="addMenu(activeComponent)">
                      {{ supportsFooter(activeComponent) ? '添加底部导航' : '添加菜单' }}
                    </el-button>
                  </div>
                </el-form-item>
              </template>
              <el-form-item v-if="supportsText(activeComponent)" label="文本内容">
                <el-input v-model="activeComponent.text" type="textarea" :rows="6" />
              </el-form-item>
              <template v-if="supportsVideo(activeComponent)">
                <el-form-item label="视频地址">
                  <el-input v-model.trim="activeComponent.videoUrl" placeholder="请输入或选择视频地址" />
                  <el-button class="mt8" @click="selectVideoFile(activeComponent)">选择视频</el-button>
                </el-form-item>
                <el-form-item label="视频封面">
                  <div class="image-field config-image-field">
                    <el-image v-if="activeComponent.coverImage" class="form-image-preview" :src="assetUrl(activeComponent.coverImage)" fit="cover" />
                    <span v-else class="form-image-empty">无图</span>
                    <el-input v-model.trim="activeComponent.coverImage" placeholder="封面图片地址" />
                    <el-button @click="selectVideoCover(activeComponent)">选择</el-button>
                  </div>
                </el-form-item>
              </template>
              <el-form-item v-if="supportsLink(activeComponent)" label="跳转链接">
                <el-input v-model.trim="activeComponent.link" placeholder="/pages/goods_details/index?id=1" />
                <el-button class="mt8" @click="openLinkSelector((link) => { activeComponent.link = link.url; activeComponent.title = activeComponent.title || link.name; })">选择链接</el-button>
              </el-form-item>
              <el-form-item v-if="supportsProduct(activeComponent)" label="商品数量">
                <el-input-number v-model="activeComponent.limit" :min="1" :max="50" />
              </el-form-item>
              <el-form-item v-if="supportsHomeTab(activeComponent)" label="展示样式">
                <el-radio-group v-model="activeComponent.itemStyleValue">
                  <el-radio-button :label="0">单列</el-radio-button>
                  <el-radio-button :label="1">两列</el-radio-button>
                  <el-radio-button :label="2">三列</el-radio-button>
                  <el-radio-button :label="3">大图</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="背景颜色">
                <el-input v-model.trim="activeComponent.bgColor" placeholder="#ffffff" />
              </el-form-item>
            </el-form>
          </template>
        </aside>
      </div>
    </el-drawer>

    <el-dialog v-model="previewVisible" title="预览" width="430px" top="6vh" destroy-on-close>
      <iframe v-if="previewVisible" class="preview-frame" :src="previewUrl" title="微页面预览" />
    </el-dialog>

    <el-dialog v-model="linkDialogVisible" title="选择链接" width="900px" append-to-body destroy-on-close>
      <div class="link-picker">
        <aside>
          <button
            v-for="tab in linkTabs"
            :key="tab.value"
            type="button"
            :class="{ active: linkQuery.type === tab.value }"
            @click="switchLinkType(tab.value)"
          >
            {{ tab.label }}
          </button>
        </aside>
        <section>
          <div class="link-search">
            <el-input v-model.trim="linkQuery.keyword" size="small" placeholder="搜索名称" clearable @keyup.enter="searchLinkRows" />
            <el-button size="small" type="primary" @click="searchLinkRows">搜索</el-button>
          </div>
          <el-table v-loading="linkLoading" :data="linkRows" size="small" height="430">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column v-if="activeLinkTab?.image" label="图片" width="82">
              <template #default="{ row }">
                <el-image v-if="row.image" class="link-row-image" :src="assetUrl(row.image)" fit="cover" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="名称" min-width="220">
              <template #default="{ row }">{{ row.name }}</template>
            </el-table-column>
            <el-table-column prop="url" label="链接" min-width="320" show-overflow-tooltip />
            <el-table-column width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="selectLink(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="activeLinkTab?.paged" class="link-pagination">
            <el-pagination
              v-model:current-page="linkQuery.page"
              v-model:page-size="linkQuery.limit"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              :total="linkTotal"
              background
              @size-change="loadLinkRows"
              @current-change="loadLinkRows"
            />
          </div>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" title="选择图片" width="860px" append-to-body destroy-on-close>
      <div class="attachment-picker">
        <aside class="attachment-tree">
          <el-tree
            :data="attachmentTree"
            node-key="id"
            :props="attachmentTreeProps"
            default-expand-all
            highlight-current
            @node-click="handleAttachmentCategory"
          />
        </aside>
        <section class="attachment-main">
          <div v-loading="attachmentLoading" class="attachment-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.attId || item.id"
              type="button"
              class="attachment-item"
              @click="selectAttachment(item)"
            >
              <el-image :src="assetUrl(item.sattDir || item.attDir)" fit="cover" />
              <span>{{ item.name || item.attName || '图片' }}</span>
            </button>
            <div v-if="!attachmentLoading && !attachmentRows.length" class="attachment-empty">暂无图片</div>
          </div>
          <div class="attachment-pagination">
            <el-pagination
              v-model:current-page="attachmentQuery.page"
              v-model:page-size="attachmentQuery.limit"
              layout="total, prev, pager, next"
              :total="attachmentTotal"
              background
              @current-change="loadAttachments"
            />
          </div>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import {
  attachmentList,
  bargainList,
  categoryTree,
  combinationList,
  articleList,
  pageDiyCopy,
  pageDiyDefault,
  pageDiyDelete,
  pageDiyInfo,
  pageDiyList,
  pageDiySave,
  pageDiySaveBase,
  pageDiySetDefault,
  pageDiyUpdate,
  productList,
  seckillStoreList
} from '../api';

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';
const ComponentPreview = defineComponent({
  props: {
    component: { type: Object, required: true },
    assetUrl: { type: Function, required: true }
  },
  setup(props) {
    return () => {
      const component = props.component;
      const imageItems = component.images || [];
      const menuItems = component.menus || [];
      if (component.type === 'search_box') {
        return h('div', { class: 'preview-search' }, component.title || '搜索商品');
      }
      if (component.type === 'banner') {
        return h('div', { class: 'preview-banner' }, [
          imageItems[0]?.image
            ? h('img', { src: props.assetUrl(imageItems[0].image), alt: imageItems[0].title || '' })
            : h('span', '轮播图')
        ]);
      }
      if (component.type === 'home_menu') {
        return h('div', { class: 'preview-menu-grid' }, menuItems.slice(0, 8).map((item) =>
          h('div', { class: 'preview-menu-item' }, [
            item.image ? h('img', { src: props.assetUrl(item.image), alt: item.title || '' }) : h('i', '图'),
            h('span', item.title || '菜单')
          ])
        ));
      }
      if (component.type === 'nav_bar') {
        return h('div', { class: 'preview-nav-bar' }, menuItems.slice(0, 10).map((item, index) =>
          h('span', { class: index === 0 ? 'active' : '' }, item.title || `分类${index + 1}`)
        ));
      }
      if (component.type === 'home_comb') {
        return h('div', { class: 'preview-home-comb' }, [
          h('div', { class: 'preview-home-comb-search' }, [
            component.logo ? h('img', { src: props.assetUrl(component.logo), alt: 'logo' }) : null,
            h('span', component.hotWord || '搜索商品名称')
          ]),
          menuItems.length ? h('div', { class: 'preview-home-comb-tabs' }, menuItems.slice(0, 5).map((item, index) =>
            h('span', { class: index === 0 ? 'active' : '' }, item.title || `分类${index + 1}`)
          )) : null,
          h('div', { class: 'preview-home-comb-banner' }, imageItems.slice(0, 3).map((item) =>
            item.image ? h('img', { src: props.assetUrl(item.image), alt: item.title || '' }) : h('div', '轮播')
          ))
        ]);
      }
      if (component.type === 'home_news_roll') {
        const firstNotice = (component.notices || [])[0] || {};
        return h('div', { class: 'preview-news-roll' }, [
          component.logo ? h('img', { src: props.assetUrl(component.logo), alt: 'news' }) : h('strong', '快讯'),
          h('span', firstNotice.title || component.title || '暂无公告')
        ]);
      }
      if (component.type === 'home_footer') {
        return h('div', { class: 'preview-footer-nav' }, menuItems.slice(0, 5).map((item, index) =>
          h('div', { class: index === 0 ? 'active' : '' }, [
            item.inactiveImage || item.activeImage || item.image
              ? h('img', { src: props.assetUrl(index === 0 ? (item.activeImage || item.image || item.inactiveImage) : (item.inactiveImage || item.image || item.activeImage)), alt: item.title || '' })
              : h('i', (item.title || '导').slice(0, 1)),
            h('span', item.title || '导航')
          ])
        ));
      }
      if (component.type === 'search_box') {
        return h('div', { class: 'preview-search-box' }, [
          component.logo ? h('img', { src: props.assetUrl(component.logo), alt: 'logo' }) : null,
          h('span', component.hotWord || component.hotWords?.[0]?.title || '搜索商品名称')
        ]);
      }
      if (component.type === 'home_tab') {
        return h('div', { class: 'preview-home-tab' }, [
          h('div', { class: 'preview-home-tab-nav' }, menuItems.slice(0, 6).map((item, index) =>
            h('span', { class: index === 0 ? 'active' : '' }, item.title || `选项${index + 1}`)
          )),
          previewGoods(component, '￥', 'preview-goods')
        ]);
      }
      if (component.type === 'picture_cube') {
        return h('div', { class: 'preview-picture-cube' }, imageItems.slice(0, 4).map((item) =>
          item.image ? h('img', { src: props.assetUrl(item.image), alt: item.title || '' }) : h('div', '图片')
        ));
      }
      if (component.type === 'home_hotspot') {
        return h('div', { class: 'preview-hotspot' }, [
          component.image
            ? h('img', { src: props.assetUrl(component.image), alt: component.title || '热区' })
            : h('div', { class: 'preview-hotspot-empty' }, '热区底图'),
          ...(component.hotspots || []).map((area, index) =>
            h('span', { class: 'preview-hotspot-area', style: hotspotStyle(area) }, index + 1)
          )
        ]);
      }
      if (component.type === 'z_ueditor') {
        return h('div', { class: 'preview-rich', innerHTML: component.text || '<p>富文本内容</p>' });
      }
      if (component.type === 'home_title') {
        return h('div', {
          class: 'preview-title',
          style: component.bgImage ? { backgroundImage: `url(${props.assetUrl(component.bgImage)})` } : undefined
        }, [
          h('div', [
            h('strong', component.title || '标题'),
            component.subTitle ? h('small', component.subTitle) : null
          ]),
          component.showMore && component.link ? h('span', component.rightText || '更多') : null
        ]);
      }
      if (component.type === 'z_auxiliary_line') {
        return h('div', { class: 'preview-line' });
      }
      if (component.type === 'z_auxiliary_box') {
        return h('div', { class: 'preview-space' });
      }
      if (component.type === 'home_video') {
        return h('div', { class: 'preview-video' }, [
          component.coverImage ? h('img', { src: props.assetUrl(component.coverImage), alt: component.title || '视频封面' }) : null,
          h('span', component.videoUrl || component.link || '视频地址')
        ]);
      }
      if (component.type === 'home_coupon') {
        return h('div', { class: 'preview-coupon' }, [
          h('div', { class: 'preview-section-title' }, component.title || '优惠券'),
          h('div', { class: 'preview-coupon-row' }, Array.from({ length: Math.min(Number(component.limit || 3), 4) }).map((_, index) =>
            h('div', { class: 'preview-coupon-card' }, [
              h('strong', `￥${[10, 20, 50, 100][index] || 10}`),
              h('span', '满额可用'),
              h('em', '立即领取')
            ])
          ))
        ]);
      }
      if (component.type === 'home_merchant') {
        const merchantItems = component.merchants?.length ? component.merchants : Array.from({ length: Math.min(Number(component.limit || 3), 6) }).map((_, index) => ({
          title: `推荐店铺 ${index + 1}`,
          image: component.logo || ''
        }));
        return h('div', { class: 'preview-merchant' }, [
          h('div', { class: 'preview-section-title' }, [
            h('strong', component.title || '推荐商户'),
            component.link ? h('span', '更多') : null
          ]),
          h('div', { class: 'preview-merchant-row' }, merchantItems.slice(0, Math.min(Number(component.limit || 3), 6)).map((item) =>
            h('div', { class: 'preview-merchant-card' }, [
              item.image ? h('img', { src: props.assetUrl(item.image), alt: item.title || '' }) : h('i', '店'),
              h('strong', item.title || '推荐店铺'),
              h('span', item.subTitle || item.label || '进店看看')
            ])
          ))
        ]);
      }
      if (component.type === 'home_article') {
        return h('div', { class: 'preview-article-list' }, [
          h('div', { class: 'preview-section-title' }, component.title || '文章资讯'),
          ...Array.from({ length: Math.min(Number(component.limit || 4), 6) }).map((_, index) =>
            h('div', { class: 'preview-article-item' }, [
              h('strong', `${component.cname || '文章'} ${index + 1}`),
              h('span', '商城资讯 / 查看详情')
            ])
          )
        ]);
      }
      if (['home_seckill', 'home_group', 'home_bargain'].includes(component.type)) {
        const priceLabel = component.type === 'home_group' ? '拼团价' : component.type === 'home_bargain' ? '砍价' : '秒杀价';
        return previewGoods(component, priceLabel, 'preview-activity');
      }
      return previewGoods(component, '￥');
    };
  }
});

function previewGoods(component, priceLabel, wrapClass = 'preview-goods') {
  return h('div', { class: wrapClass }, [
    h('div', { class: wrapClass === 'preview-activity' ? 'preview-activity-head' : 'preview-section-title' }, [
      h('strong', component.title || component.cname || '商品'),
      component.link ? h('span', '更多') : null
    ]),
    h('div', { class: wrapClass === 'preview-activity' ? 'preview-activity-row' : 'preview-goods-grid' }, Array.from({ length: Math.min(Number(component.limit || 4), 6) }).map((_, index) =>
      h('div', { class: wrapClass === 'preview-activity' ? 'preview-activity-card' : 'preview-goods-card' }, [
        h('div', { class: wrapClass === 'preview-activity' ? 'preview-activity-img' : 'preview-goods-img' }, '商品'),
        h('strong', `${component.cname || '商品'} ${index + 1}`),
        h('span', priceLabel === '￥' ? '￥99.00' : `${priceLabel} ￥99.00`)
      ])
    ))
  ]);
}

const activeTab = ref('home');
const loading = ref(false);
const saving = ref(false);
const designerSaving = ref(false);
const attachmentLoading = ref(false);
const linkLoading = ref(false);
const rows = ref([]);
const total = ref(0);
const drawerVisible = ref(false);
const designerVisible = ref(false);
const previewVisible = ref(false);
const attachmentDialogVisible = ref(false);
const linkDialogVisible = ref(false);
const previewUrl = ref('');
const editingId = ref(null);
const selectedIndex = ref(-1);
const draggedComponent = ref(null);
const draggedIndex = ref(null);
const linkApply = ref(null);
const attachmentApply = ref(null);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const attachmentTargetField = ref('');
const activeHotspotIndex = ref(-1);
const designerComponents = ref([]);
const linkRows = ref([]);
const linkTotal = ref(0);
const query = reactive({ page: 1, limit: 10, name: '' });
const form = reactive(defaultForm());
const designerForm = reactive(defaultForm());
const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});
const linkQuery = reactive({
  type: 'static',
  keyword: '',
  page: 1,
  limit: 10
});
const componentGroups = [
  {
    title: '基础组件',
    list: [
      { type: 'search_box', label: '搜索框', icon: '搜' },
      { type: 'banner', label: '轮播图', icon: '图' },
      { type: 'home_comb', label: '头部组合', icon: '组' },
      { type: 'home_news_roll', label: '新闻播报', icon: '讯' },
      { type: 'home_menu', label: '导航菜单', icon: '宫' },
      { type: 'nav_bar', label: '商品分类', icon: '类' },
      { type: 'home_tab', label: '商品选项卡', icon: '签' },
      { type: 'picture_cube', label: '图片魔方', icon: '拼' },
      { type: 'home_hotspot', label: '热区', icon: '区' },
      { type: 'home_footer', label: '底部导航', icon: '底' },
      { type: 'home_title', label: '标题', icon: '题' },
      { type: 'z_ueditor', label: '富文本', icon: '文' },
      { type: 'z_auxiliary_line', label: '辅助线', icon: '线' },
      { type: 'z_auxiliary_box', label: '辅助空白', icon: '空' }
    ]
  },
  {
    title: '营销组件',
    list: [
      { type: 'home_goods_list', label: '商品列表', icon: '商' },
      { type: 'home_coupon', label: '优惠券', icon: '券' },
      { type: 'home_merchant', label: '推荐商户', icon: '店' },
      { type: 'home_seckill', label: '秒杀', icon: '秒' },
      { type: 'home_group', label: '拼团', icon: '团' },
      { type: 'home_bargain', label: '砍价', icon: '砍' },
      { type: 'home_article', label: '文章', icon: '文' },
      { type: 'home_video', label: '视频', icon: '播' }
    ]
  }
];
const linkTabs = [
  { value: 'static', label: '商城页面' },
  { value: 'product', label: '商品', image: true, paged: true },
  { value: 'category', label: '分类', image: true },
  { value: 'seckill', label: '秒杀商品', image: true, paged: true },
  { value: 'combination', label: '拼团商品', image: true, paged: true },
  { value: 'bargain', label: '砍价商品', image: true, paged: true },
  { value: 'article', label: '文章', image: true, paged: true },
  { value: 'page', label: '微页面', paged: true }
];
const activeComponent = computed(() => selectedIndex.value >= 0 ? designerComponents.value[selectedIndex.value] : null);
const activeLinkTab = computed(() => linkTabs.find((item) => item.value === linkQuery.type));

const frontDomain = computed(() => {
  const value = localStorage.getItem('frontDomain') || 'http://127.0.0.1:19528';
  return /^https?:\/\//.test(value) ? value : `https://${value}`;
});
const drawerTitle = computed(() => (editingId.value ? '页面基础信息' : '添加页面'));
const attachmentTree = computed(() => [
  {
    id: 0,
    name: '全部图片',
    child: attachmentCategories.value
  }
]);
const attachmentTreeProps = {
  children: 'child',
  label: 'name'
};

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await pageDiyList(compactParams(query));
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

async function editHome() {
  const id = await pageDiyDefault();
  activeTab.value = 'pages';
  editPage(id);
}

async function editPage(id) {
  const data = await pageDiyInfo(id);
  editingId.value = data.id;
  Object.assign(form, defaultForm(), {
    id: data.id,
    name: data.name || '',
    title: data.title || '首页',
    coverImage: data.coverImage || '',
    templateName: data.templateName || '',
    value: data.value || '{}',
    status: numberOr(data.status, 0),
    type: numberOr(data.type, 0),
    isShow: numberOr(data.isShow, 1),
    isBgColor: numberOr(data.isBgColor, 0),
    isBgPic: numberOr(data.isBgPic, 0),
    isDiy: numberOr(data.isDiy, 0),
    colorPicker: data.colorPicker || '',
    bgPic: data.bgPic || '',
    bgTabVal: numberOr(data.bgTabVal, 0),
    returnAddress: data.returnAddress || '0',
    titleBgColor: data.titleBgColor || '1',
    titleColor: data.titleColor || '1',
    serviceStatus: numberOr(data.serviceStatus, 1),
    textPosition: numberOr(data.textPosition, 0)
  });
  drawerVisible.value = true;
}

async function openDesigner(id) {
  const data = await pageDiyInfo(id);
  Object.assign(designerForm, defaultForm(), {
    id: data.id,
    name: data.name || '',
    title: data.title || '首页',
    coverImage: data.coverImage || '',
    templateName: data.templateName || '',
    value: data.value || '{}',
    status: numberOr(data.status, 0),
    type: numberOr(data.type, 0),
    isShow: numberOr(data.isShow, 1),
    isBgColor: numberOr(data.isBgColor, 0),
    isBgPic: numberOr(data.isBgPic, 0),
    isDiy: 1,
    colorPicker: data.colorPicker || '',
    bgPic: data.bgPic || '',
    bgTabVal: numberOr(data.bgTabVal, 0),
    returnAddress: data.returnAddress || '0',
    titleBgColor: data.titleBgColor || '1',
    titleColor: data.titleColor || '1',
    serviceStatus: numberOr(data.serviceStatus, 1),
    textPosition: numberOr(data.textPosition, 0)
  });
  designerComponents.value = parsePageValue(data.value);
  selectedIndex.value = designerComponents.value.length ? 0 : -1;
  designerVisible.value = true;
}

function createPage() {
  editingId.value = null;
  Object.assign(form, defaultForm());
  drawerVisible.value = true;
}

async function saveDesigner(closeAfterSave) {
  if (!designerForm.name) {
    ElMessage.warning('请输入模板名称');
    return;
  }
  designerSaving.value = true;
  try {
    const payload = compactParams({
      ...designerForm,
      isDiy: 1,
      value: JSON.stringify(buildPageValue())
    });
    const data = designerForm.id ? await pageDiyUpdate(payload) : await pageDiySave(payload);
    ElMessage.success('保存成功');
    if (data?.id) {
      designerForm.id = data.id;
    }
    if (closeAfterSave) {
      designerVisible.value = false;
    }
    await loadList();
  } finally {
    designerSaving.value = false;
  }
}

function resetDesigner() {
  Object.assign(designerForm, defaultForm());
  designerComponents.value = [];
  selectedIndex.value = -1;
  draggedComponent.value = null;
  draggedIndex.value = null;
}

function addComponent(component) {
  designerComponents.value.push(createComponent(component));
  selectedIndex.value = designerComponents.value.length - 1;
}

function createComponent(component) {
  const defaults = componentDefaults(component.type);
  const uid = `${component.type}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const base = {
    uid,
    id: component.type,
    name: component.type,
    type: component.type,
    cname: component.label,
    label: component.label,
    title: component.label,
    num: designerComponents.value.length,
    isHide: false,
    bgColor: '',
    link: defaults.link,
    text: '',
    limit: defaults.limit,
    images: [],
    menus: []
  };
  if (component.type === 'banner' || component.type === 'picture_cube') {
    base.images = [emptyImage('图片1')];
  }
  if (component.type === 'home_hotspot') {
    base.image = '';
    base.images = [emptyImage('热区底图')];
    base.hotspots = [emptyHotspot('热区1')];
  }
  if (component.type === 'home_comb') {
    base.logo = '';
    base.hotWord = '搜索商品名称';
    base.images = [emptyImage('轮播1'), emptyImage('轮播2'), emptyImage('轮播3')];
    base.menus = ['精选', '热门', '新品', '促销'].map((title, index) => emptyMenu(title, index));
  }
  if (component.type === 'search_box') {
    base.logo = '';
    base.hotWord = '搜索商品名称';
    base.hotWords = [emptyHotWord('热销商品')];
    base.link = '/pages/goods/goods_search/index';
  }
  if (component.type === 'home_tab') {
    base.menus = ['推荐', '新品', '热销'].map((title, index) => emptyMenu(title, index));
    base.limit = 6;
    base.itemStyleValue = 1;
  }
  if (component.type === 'home_news_roll') {
    base.logo = '';
    base.notices = [emptyNotice('商城公告')];
  }
  if (component.type === 'home_merchant') {
    base.logo = '';
    base.title = '推荐商户';
    base.link = '/pages/goods/goods_list/index';
    base.limit = 3;
    base.merchants = [
      emptyMerchant('精选好店', 0),
      emptyMerchant('人气店铺', 1),
      emptyMerchant('新品店铺', 2)
    ];
  }
  if (component.type === 'home_menu') {
    base.menus = ['首页', '分类', '购物车', '我的'].map((title, index) => emptyMenu(title, index));
  }
  if (component.type === 'nav_bar') {
    base.menus = ['精选', '热销', '新品', '促销'].map((title, index) => emptyMenu(title, index));
  }
  if (component.type === 'home_footer') {
    base.menus = ['首页', '分类', '购物车', '我的'].map((title, index) => emptyFooterMenu(title, index));
  }
  if (component.type === 'z_ueditor') {
    base.text = '<p>请输入富文本内容</p>';
  }
  if (component.type === 'home_title') {
    base.title = '标题';
    base.subTitle = '副标题';
    base.rightText = '更多';
    base.showMore = true;
    base.bgImage = '';
    base.text = '';
  }
  if (component.type === 'home_video') {
    base.videoUrl = '';
    base.coverImage = '';
  }
  return base;
}

function parsePageValue(value) {
  if (!value) {
    return [];
  }
  let parsed;
  try {
    parsed = typeof value === 'string' ? JSON.parse(value) : value;
  } catch {
    return [];
  }
  const list = Array.isArray(parsed) ? parsed : Array.isArray(parsed?.list) ? parsed.list : Object.values(parsed || {});
  return list
    .filter((item) => item && typeof item === 'object')
    .map((item, index) => normalizeComponent(item, index));
}

function normalizeComponent(item, index) {
  const type = normalizeDiyType(item.type || item.name || item.id || 'custom');
  const label = item.cname || item.label || componentLabel(type);
  const images = normalizeItems(
    item.images || item.imgList || item.picList || item.list || item.pic || item.swiperConfig?.list || item.picStyle?.picList
  );
  const menus = normalizeItems(item.menus || item.menuList?.list || item.menuList || item.navList || item.listConfig?.list || item.menuConfig?.list || item.tabConfig?.list);
  const notices = normalizeItems(item.notices || item.newsList || item.newsConfig?.list || item.listConfig?.list);
  const merchants = normalizeMerchants(item.merchants || item.merchantList || item.merList || item.activeValueMer?.activeValue || item.activeValueMer?.list);
  const hotspots = normalizeHotspots(item.hotspots || item.checkoutConfig?.hotspot || item.areaData || item.imgAreaData);
  const hotspotImage = item.image || item.picStyle?.picList?.[0]?.image || item.picStyle?.picList?.[0]?.img || images[0]?.image || '';
  const titleLink = typeof item.link === 'object' ? item.link?.value : item.link;
  const hotWords = normalizeHotWords(item.hotWords?.list || item.hotWords || item.hotWordList);
  const activeTab = item.tabItemConfig?.list?.[Number(item.tabItemConfig?.tabVal || 0)]?.activeList || {};
  const titleShowMore = item.showMore !== undefined
    ? Boolean(item.showMore)
    : item.selectShow?.tabVal !== undefined
      ? Number(item.selectShow.tabVal) === 0
      : Boolean(item.titleRightConfig?.val || titleLink || item.linkConfig?.val || item.url);
  return {
    ...item,
    uid: item.uid || `${type}-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
    id: item.id || type,
    name: item.name || type,
    type,
    cname: label,
    label,
    title: item.titleConfig?.val || item.title || item.nameText || item.txt || label,
    subTitle: item.subTitle || item.subtitle || item.titleFuConfig?.val || '',
    rightText: item.rightText || item.moreText || item.titleRightConfig?.val || '更多',
    showMore: titleShowMore,
    bgImage: item.bgImage || item.bgImg?.url || '',
    num: index,
    isHide: Boolean(item.isHide),
    bgColor: item.bgColor || item.backgroundColor || '',
    link: titleLink || item.url || item.linkConfig?.val || '',
    text: item.text || item.content || item.html || '',
    videoUrl: item.videoUrl || item.uploadVideo?.url || item.link?.value || item.video || '',
    coverImage: item.coverImage || item.cover?.url || item.cover?.val || '',
    limit: numberOr(item.limit || item.numConfig?.val || item.numConfig || item.count || activeTab.num, 4),
    itemStyleValue: numberOr(item.itemStyle?.tabVal || item.itemStyleValue || item.itemStyle || activeTab.styleType, 0),
    images,
    menus,
    notices,
    merchants,
    logo: item.logo || item.logoConfig?.url || '',
    hotWord: item.hotWord || item.placeWords?.value || hotWords[0]?.title || '',
    hotWords,
    image: hotspotImage,
    hotspots
  };
}

function normalizeItems(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === 'object' ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const info = Array.isArray(item.info) ? item.info : [];
    const child = Array.isArray(item.child) ? item.child : Array.isArray(item.chiild) ? item.chiild : [];
    return {
      uid: item.uid || `item-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
      title: item.title || item.name || item.val || info[0]?.value || info[0]?.val || child[0]?.val || `项目${index + 1}`,
      image: item.image || item.pic || item.img || item.url || item.info?.image || item.checked || item.unchecked || '',
      link: item.link || item.path || item.href || item.urlLink || item.val || info[1]?.value || info[0]?.value || child[1]?.val || ''
    };
  });
}

function normalizeHotWords(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === 'object' ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => ({
    uid: item.uid || `hot-word-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
    title: typeof item === 'string' ? item : item.title || item.val || item.value || `热词${index + 1}`
  }));
}

function buildPageValue() {
  return designerComponents.value.reduce((result, component, index) => {
    const payload = {
      ...component,
      num: index,
      name: component.type || component.name,
      cname: component.cname || component.label
    };
    if (component.type === 'home_hotspot') {
      payload.name = 'homeHotspot';
      payload.image = component.image || component.images?.[0]?.image || '';
      payload.images = [hotspotImageItem(component)];
      payload.picStyle = {
        ...(component.picStyle || {}),
        isHotspot: 1,
        picList: [{ image: payload.image, link: component.link || '' }]
      };
      payload.checkoutConfig = {
        ...(component.checkoutConfig || {}),
        title: '图片设置',
        checkoutVal: '2',
        hotspot: normalizeHotspots(component.hotspots)
      };
    }
    if (component.type === 'home_comb') {
      payload.name = 'homeComb';
      payload.logoConfig = {
        ...(component.logoConfig || {}),
        isShow: component.logo ? 1 : 0,
        url: component.logo || ''
      };
      payload.placeWords = {
        ...(component.placeWords || {}),
        value: component.hotWord || ''
      };
      payload.hotWords = {
        ...(component.hotWords || {}),
        list: [{ val: component.hotWord || '' }]
      };
      payload.listConfig = {
        ...(component.listConfig || {}),
        list: (component.menus || []).map((item) => ({
          title: item.title || '',
          val: item.link || '',
          url: item.link || '',
          type: 0
        }))
      };
      payload.swiperConfig = {
        ...(component.swiperConfig || {}),
        list: (component.images || []).map((item) => ({
          img: item.image || '',
          info: [
            { title: '标题', value: item.title || '', max: 8 },
            { title: '链接', value: item.link || '', max: 100 }
          ]
        }))
      };
    }
    if (component.type === 'search_box') {
      payload.name = 'headerSerch';
      payload.logoConfig = {
        ...(component.logoConfig || {}),
        isShow: component.logo ? 1 : 0,
        title: component.logoConfig?.title || '默认logo',
        tips: component.logoConfig?.tips || '建议上传大小：宽138px，高48px',
        header: component.logoConfig?.header || '设置logo',
        url: component.logo || ''
      };
      payload.logoFixConfig = {
        ...(component.logoFixConfig || {}),
        isShow: component.logo ? 1 : 0,
        title: component.logoFixConfig?.title || '吸顶logo',
        tips: component.logoFixConfig?.tips || '建议上传大小：宽138px，高48px',
        header: component.logoFixConfig?.header || '设置logo',
        url: component.logo || ''
      };
      payload.placeWords = {
        ...(component.placeWords || {}),
        title: component.placeWords?.title || '提示文字',
        val: component.hotWord || '',
        value: component.hotWord || '',
        isShow: 1
      };
      payload.hotWords = {
        ...(component.hotWordsConfig || {}),
        title: component.hotWordsConfig?.title || '搜索热词',
        tabTitle: component.hotWordsConfig?.tabTitle || '搜索热词',
        list: (component.hotWords || []).slice(0, 20).map((item) => ({ val: item.title || item.val || '' }))
      };
      payload.searConfig = {
        ...(component.searConfig || {}),
        title: component.searConfig?.title || '选择模板',
        tabTitle: component.searConfig?.tabTitle || '布局设置',
        tabVal: component.logo ? 1 : 0,
        isShow: 1,
        list: component.searConfig?.list || [{ val: '通栏', count: 1 }, { val: 'logo', count: 2 }]
      };
      payload.link = component.link || '/pages/goods/goods_search/index';
    }
    if (component.type === 'home_news_roll') {
      payload.name = 'news';
      payload.logoConfig = {
        ...(component.logoConfig || {}),
        url: component.logo || ''
      };
      payload.listConfig = {
        ...(component.listConfig || {}),
        max: 10,
        list: (component.notices || []).map((item) => ({
          status: true,
          chiild: [
            { title: '标题', val: item.title || '', max: 30, pla: '选填，不超过30个字', empty: true },
            { title: '链接', val: item.link || '', max: 100, pla: '请选择链接', empty: true }
          ]
        }))
      };
    }
    if (component.type === 'home_footer') {
      payload.name = 'footer';
      payload.menuList = {
        ...(component.menuList || {}),
        tabTitle: component.menuList?.tabTitle || '内容设置',
        tabVal: component.menuList?.tabVal || 0,
        list: (component.menus || []).slice(0, 5).map((item) => ({
          checked: item.activeImage || item.image || '',
          unchecked: item.inactiveImage || item.image || '',
          name: item.title || '',
          link: item.link || ''
        }))
      };
      payload.footerItems = payload.menuList.list.map((item, itemIndex) => ({
        uid: `footer-${index}-${itemIndex}`,
        title: item.name,
        activeImage: item.checked,
        inactiveImage: item.unchecked,
        link: item.link
      }));
    }
    if (component.type === 'home_merchant') {
      payload.name = 'homeMerchant';
      payload.logoConfig = {
        ...(component.logoConfig || {}),
        isShow: component.logo ? 1 : 0,
        title: component.logoConfig?.title || '好店图标',
        tips: component.logoConfig?.tips || '建议上传方形店铺图标',
        url: component.logo || ''
      };
      payload.titleConfig = {
        ...(component.titleConfig || {}),
        tabTitle: component.titleConfig?.tabTitle || '标题设置',
        title: component.titleConfig?.title || '标题名称',
        val: component.title || '推荐商户',
        isShow: 1
      };
      payload.linkConfig = {
        ...(component.linkConfig || {}),
        title: component.linkConfig?.title || '更多链接',
        val: component.link || '',
        isShow: 1
      };
      payload.listConfig = {
        ...(component.listConfig || {}),
        title: component.listConfig?.title || '商户来源',
        tabVal: component.listConfig?.tabVal || 0,
        isShow: 1
      };
      payload.numConfig = {
        ...(component.numConfig || {}),
        val: Number(component.limit || 3),
        isShow: 1,
        max: 20
      };
      payload.activeValueMer = {
        ...(component.activeValueMer || {}),
        title: component.activeValueMer?.title || '指定商户',
        isShow: 1,
        activeValue: (component.merchants || []).map((item) => item.id || item.value || item.title).filter(Boolean),
        list: (component.merchants || []).map((item) => ({
          id: item.id || '',
          name: item.title || '',
          image: item.image || '',
          link: item.link || '',
          label: item.label || item.subTitle || ''
        }))
      };
      payload.typeConfig = {
        ...(component.typeConfig || {}),
        title: component.typeConfig?.title || '展示信息',
        activeValue: component.typeConfig?.activeValue || [0, 1, 2]
      };
    }
    if (component.type === 'nav_bar') {
      payload.name = 'tabNav';
      payload.listConfig = {
        ...(component.listConfig || {}),
        title: component.listConfig?.title || '鼠标拖拽左侧圆点可调整选项卡顺序',
        tabTitle: component.listConfig?.tabTitle || '选项卡设置',
        max: 10,
        list: (component.menus || []).slice(0, 10).map((item) => ({
          title: item.title || '',
          val: item.link || '',
          url: item.link || '',
          type: 0
        }))
      };
    }
    if (component.type === 'home_tab') {
      payload.name = 'homeTab';
      payload.tabItemConfig = {
        ...(component.tabItemConfig || {}),
        tabTitle: component.tabItemConfig?.tabTitle || '选项卡设置',
        title: component.tabItemConfig?.title || '选项卡设置',
        name: 'tabItemConfig',
        tabVal: 0,
        list: (component.menus || []).map((item, itemIndex) => ({
          val: item.title || `选项卡${itemIndex + 1}`,
          link: item.link || '',
          activeTabIndex: itemIndex,
          brandGoodsList: [],
          selectConfigList: [],
          merList: [],
          selectCate: [],
          activeList: {
            goods: [],
            activeProTabIndex: 1,
            activeValue: '',
            styleType: Number(component.itemStyleValue || 0),
            num: Number(component.limit || 6),
            goodsSort: 0,
            showContent: [0, 1, 2]
          }
        }))
      };
      payload.tabConfig = {
        ...(component.tabConfig || {}),
        tabTitle: component.tabConfig?.tabTitle || '商品设置',
        title: component.tabConfig?.title || '商品列表',
        tabVal: 1,
        isShow: 1,
        list: component.tabConfig?.list || [{ name: '指定商品' }, { name: '指定分类' }]
      };
      payload.itemStyle = {
        ...(typeof component.itemStyle === 'object' ? component.itemStyle : {}),
        tabTitle: component.itemStyle?.tabTitle || '展现形式',
        title: component.itemStyle?.title || '展现样式',
        name: 'itemSstyle',
        tabVal: Number(component.itemStyleValue || 0),
        isShow: 1
      };
      payload.typeConfig = {
        ...(component.typeConfig || {}),
        title: component.typeConfig?.title || '展示信息',
        tabTitle: component.typeConfig?.tabTitle || '显示内容',
        activeValue: component.typeConfig?.activeValue || [0, 1, 2]
      };
      payload.numConfig = {
        ...(component.numConfig || {}),
        val: Number(component.limit || 6),
        isShow: 1,
        max: 50
      };
      payload.goodsSort = {
        ...(component.goodsSort || {}),
        title: component.goodsSort?.title || '商品排序',
        name: 'goodsSort',
        tabVal: component.goodsSort?.tabVal || 0,
        isShow: 1
      };
    }
    if (component.type === 'home_title') {
      payload.name = 'titles';
      payload.titleConfig = {
        ...(component.titleConfig || {}),
        tabTitle: component.titleConfig?.tabTitle || '文本标题',
        title: component.titleConfig?.title || '标题名称',
        val: component.title || '',
        place: component.titleConfig?.place || '请输入标题',
        max: component.titleConfig?.max || 16,
        isShow: 1
      };
      payload.titleFuConfig = {
        ...(component.titleFuConfig || {}),
        tabTitle: component.titleFuConfig?.tabTitle || '文本标题',
        title: component.titleFuConfig?.title || '副标题',
        val: component.subTitle || '',
        place: component.titleFuConfig?.place || '请输入副标题',
        max: component.titleFuConfig?.max || 24,
        isShow: 1
      };
      payload.titleRightConfig = {
        ...(component.titleRightConfig || {}),
        tabTitle: component.titleRightConfig?.tabTitle || '右侧文字',
        title: component.titleRightConfig?.title || '右侧文字',
        val: component.rightText || '更多',
        place: component.titleRightConfig?.place || '请输入右侧文字',
        max: component.titleRightConfig?.max || 8,
        isShow: component.showMore ? 1 : 0
      };
      payload.linkConfig = {
        ...(component.linkConfig || {}),
        tabTitle: component.linkConfig?.tabTitle || '右侧链接',
        title: component.linkConfig?.title || '链接',
        val: component.link || '',
        place: component.linkConfig?.place || '请选择链接',
        isShow: component.showMore ? 1 : 0
      };
      payload.bgImg = {
        ...(component.bgImg || {}),
        isShow: component.bgImage ? 1 : 0,
        title: component.bgImg?.title || '上传背景图',
        tips: component.bgImg?.tips || '建议：910px*86px',
        url: component.bgImage || ''
      };
      payload.selectShow = {
        ...(component.selectShow || {}),
        cname: 'selectShow',
        title: component.selectShow?.title || '右侧按钮',
        tabVal: component.showMore ? 0 : 1,
        isShow: 1,
        list: component.selectShow?.list || [{ val: '显示' }, { val: '隐藏' }]
      };
    }
    if (component.type === 'home_video') {
      payload.name = 'video';
      payload.videoUrl = component.videoUrl || component.link || '';
      payload.coverImage = component.coverImage || '';
      payload.uploadVideo = {
        ...(component.uploadVideo || {}),
        title: component.uploadVideo?.title || '上传视频',
        name: 'uploadVideo',
        url: payload.videoUrl
      };
      payload.link = {
        ...(typeof component.link === 'object' ? component.link : {}),
        title: component.link?.title || '视频链接',
        value: payload.videoUrl,
        place: component.link?.place || '请输入链接地址'
      };
      payload.cover = {
        ...(component.cover || {}),
        name: 'cover',
        url: payload.coverImage,
        val: payload.coverImage,
        title: component.cover?.title || '视频封面'
      };
    }
    result[index] = payload;
    return result;
  }, {});
}

function selectPageSetting() {
  selectedIndex.value = -1;
}

function selectComponent(index) {
  selectedIndex.value = index;
}

function moveComponent(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= designerComponents.value.length) {
    return;
  }
  const next = [...designerComponents.value];
  [next[index], next[target]] = [next[target], next[index]];
  designerComponents.value = renumber(next);
  selectedIndex.value = target;
}

function copyComponent(index) {
  const source = designerComponents.value[index];
  if (!source) {
    return;
  }
  const copy = JSON.parse(JSON.stringify(source));
  copy.uid = `${copy.type}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  designerComponents.value.splice(index + 1, 0, copy);
  designerComponents.value = renumber(designerComponents.value);
  selectedIndex.value = index + 1;
}

function toggleComponent(index) {
  const component = designerComponents.value[index];
  if (component) {
    component.isHide = !component.isHide;
  }
}

function removeComponent(index) {
  designerComponents.value.splice(index, 1);
  designerComponents.value = renumber(designerComponents.value);
  selectedIndex.value = Math.min(index, designerComponents.value.length - 1);
  if (selectedIndex.value < 0) {
    selectedIndex.value = -1;
  }
}

function dragComponent(component) {
  draggedComponent.value = component;
  draggedIndex.value = null;
}

function dragExisting(index) {
  draggedIndex.value = index;
  draggedComponent.value = null;
}

function dropComponentAt(index) {
  if (draggedComponent.value) {
    const next = [...designerComponents.value];
    next.splice(index, 0, createComponent(draggedComponent.value));
    designerComponents.value = renumber(next);
    selectedIndex.value = index;
  } else if (draggedIndex.value !== null && draggedIndex.value !== index) {
    const next = [...designerComponents.value];
    const [item] = next.splice(draggedIndex.value, 1);
    const target = draggedIndex.value < index ? index - 1 : index;
    next.splice(target, 0, item);
    designerComponents.value = renumber(next);
    selectedIndex.value = target;
  }
  draggedComponent.value = null;
  draggedIndex.value = null;
}

function renumber(list) {
  return list.map((item, index) => ({ ...item, num: index }));
}

function supportsImages(component) {
  return ['banner', 'picture_cube', 'home_comb'].includes(component.type);
}

function supportsSearchBox(component) {
  return component.type === 'search_box';
}

function supportsHomeComb(component) {
  return component.type === 'home_comb';
}

function supportsNewsRoll(component) {
  return component.type === 'home_news_roll';
}

function supportsHotspots(component) {
  return component.type === 'home_hotspot';
}

function supportsFooter(component) {
  return component.type === 'home_footer';
}

function supportsMenus(component) {
  return ['home_menu', 'home_comb', 'home_footer', 'nav_bar'].includes(component.type);
}

function supportsMerchants(component) {
  return component.type === 'home_merchant';
}

function supportsHomeTab(component) {
  return component.type === 'home_tab';
}

function supportsText(component) {
  return component.type === 'z_ueditor';
}

function supportsTitle(component) {
  return component.type === 'home_title';
}

function supportsVideo(component) {
  return component.type === 'home_video';
}

function supportsLink(component) {
  return ['search_box', 'home_title', 'home_video', 'home_comb', 'home_goods_list', 'home_coupon', 'home_merchant', 'home_seckill', 'home_group', 'home_bargain', 'home_article'].includes(component.type);
}

function supportsProduct(component) {
  return ['home_goods_list', 'home_coupon', 'home_merchant', 'home_seckill', 'home_group', 'home_bargain', 'home_article', 'home_tab'].includes(component.type);
}

function addImage(component) {
  component.images = [...(component.images || []), emptyImage(`图片${(component.images || []).length + 1}`)];
}

function removeImage(component, index) {
  component.images.splice(index, 1);
}

function selectImageFor(component, index) {
  openAttachmentSelector('', (item) => {
    component.images[index].image = item.sattDir || item.attDir || '';
  });
}

function addMenu(component) {
  if (supportsFooter(component) && (component.menus || []).length >= 5) {
    return;
  }
  const index = (component.menus || []).length;
  const nextItem = supportsFooter(component) ? emptyFooterMenu(`导航${index + 1}`, index) : emptyMenu(`菜单${index + 1}`, index);
  component.menus = [...(component.menus || []), nextItem];
}

function removeMenu(component, index) {
  component.menus.splice(index, 1);
}

function addHotWord(component) {
  component.hotWords = [...(component.hotWords || []), emptyHotWord(`热词${(component.hotWords || []).length + 1}`)];
}

function removeHotWord(component, index) {
  component.hotWords.splice(index, 1);
}

function selectMenuImage(component, index) {
  openAttachmentSelector('', (item) => {
    component.menus[index].image = item.sattDir || item.attDir || '';
  });
}

function selectFooterImage(component, index, field) {
  openAttachmentSelector('', (item) => {
    component.menus[index][field] = item.sattDir || item.attDir || '';
  });
}

function selectVideoFile(component) {
  openAttachmentSelector('', (item) => {
    component.videoUrl = item.sattDir || item.attDir || '';
  });
}

function selectVideoCover(component) {
  openAttachmentSelector('', (item) => {
    component.coverImage = item.sattDir || item.attDir || '';
  });
}

function selectHomeCombLogo(component) {
  openAttachmentSelector('', (item) => {
    component.logo = item.sattDir || item.attDir || '';
  });
}

function selectNewsLogo(component) {
  openAttachmentSelector('', (item) => {
    component.logo = item.sattDir || item.attDir || '';
  });
}

function selectSearchLogo(component) {
  openAttachmentSelector('', (item) => {
    component.logo = item.sattDir || item.attDir || '';
  });
}

function selectTitleBg(component) {
  openAttachmentSelector('', (item) => {
    component.bgImage = item.sattDir || item.attDir || '';
  });
}

function addNotice(component) {
  component.notices = [...(component.notices || []), emptyNotice(`公告${(component.notices || []).length + 1}`)];
}

function removeNotice(component, index) {
  component.notices.splice(index, 1);
}

function addMerchant(component) {
  component.merchants = [...(component.merchants || []), emptyMerchant(`推荐店铺${(component.merchants || []).length + 1}`, (component.merchants || []).length)];
}

function removeMerchant(component, index) {
  component.merchants.splice(index, 1);
}

function selectMerchantImage(component, index) {
  openAttachmentSelector('', (item) => {
    component.merchants[index].image = item.sattDir || item.attDir || '';
  });
}

function selectMerchantLogo(component) {
  openAttachmentSelector('', (item) => {
    component.logo = item.sattDir || item.attDir || '';
  });
}

function selectHotspotImage(component) {
  openAttachmentSelector('', (item) => {
    component.image = item.sattDir || item.attDir || '';
    component.images = [hotspotImageItem(component)];
  });
}

function addHotspot(component) {
  component.hotspots = [...(component.hotspots || []), emptyHotspot(`热区${(component.hotspots || []).length + 1}`)];
  activeHotspotIndex.value = component.hotspots.length - 1;
}

function removeHotspot(component, index) {
  component.hotspots.splice(index, 1);
  activeHotspotIndex.value = Math.min(index, component.hotspots.length - 1);
}

async function savePage() {
  if (!form.name) {
    ElMessage.warning('请输入模板名称');
    return;
  }
  saving.value = true;
  try {
    const data = await pageDiySaveBase(compactParams(form));
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    if (!editingId.value && data?.id) {
      query.page = 1;
    }
    loadList();
  } finally {
    saving.value = false;
  }
}

async function setHomepage(id) {
  await ElMessageBox.confirm('把该模板设为首页？', '提示', { type: 'warning' });
  await pageDiySetDefault(id);
  ElMessage.success('操作成功');
  loadList();
}

async function copyPage(id) {
  await pageDiyCopy(id);
  ElMessage.success('复制成功');
  query.page = 1;
  loadList();
}

async function deletePage(id) {
  await ElMessageBox.confirm('删除模板吗？', '提示', { type: 'warning' });
  await pageDiyDelete({ id });
  ElMessage.success('删除成功');
  if (rows.value.length === 1 && query.page > 1) query.page -= 1;
  loadList();
}

function previewPage(id) {
  const glue = frontDomain.value.includes('?') ? '&' : '?';
  previewUrl.value = `${frontDomain.value}${glue}id=${id}`;
  previewVisible.value = true;
}

function resetForm() {
  editingId.value = null;
  Object.assign(form, defaultForm());
}

async function openAttachmentSelector(field, apply = null) {
  attachmentTargetField.value = field;
  attachmentApply.value = apply;
  attachmentDialogVisible.value = true;
  attachmentQuery.page = 1;
  attachmentQuery.pid = 0;
  if (!attachmentCategories.value.length) {
    attachmentCategories.value = await categoryTree({ type: 2, status: -1 });
  }
  await loadAttachments();
}

async function loadAttachments() {
  attachmentLoading.value = true;
  try {
    const data = await attachmentList({ ...attachmentQuery });
    attachmentRows.value = data?.list || [];
    attachmentTotal.value = Number(data?.total || 0);
  } finally {
    attachmentLoading.value = false;
  }
}

function handleAttachmentCategory(data) {
  attachmentQuery.pid = data.id;
  attachmentQuery.page = 1;
  loadAttachments();
}

function selectAttachment(item) {
  if (attachmentApply.value) {
    attachmentApply.value(item);
    attachmentApply.value = null;
    attachmentDialogVisible.value = false;
    return;
  }
  if (!attachmentTargetField.value) return;
  form[attachmentTargetField.value] = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
}

function openLinkSelector(apply) {
  linkApply.value = apply;
  linkDialogVisible.value = true;
  linkQuery.keyword = '';
  linkQuery.page = 1;
  loadLinkRows();
}

function switchLinkType(type) {
  linkQuery.type = type;
  linkQuery.keyword = '';
  linkQuery.page = 1;
  linkRows.value = [];
  linkTotal.value = 0;
  loadLinkRows();
}

function searchLinkRows() {
  linkQuery.page = 1;
  loadLinkRows();
}

async function loadLinkRows() {
  linkLoading.value = true;
  try {
    const keyword = linkQuery.keyword.trim();
    if (linkQuery.type === 'static') {
      linkRows.value = staticLinks()
        .filter((item) => !keyword || item.name.includes(keyword) || item.url.includes(keyword))
        .map((item) => ({ ...item, id: item.id || item.url }));
      linkTotal.value = linkRows.value.length;
    } else if (linkQuery.type === 'product') {
      const data = await productList({ keywords: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.storeName || item.store_name || `商品${item.id}`,
        image: item.image,
        url: `/pages/goods/goods_details/index?id=${item.id}`
      }));
      linkTotal.value = Number(data?.total || 0);
    } else if (linkQuery.type === 'category') {
      const tree = await categoryTree({ type: 1, status: -1 });
      linkRows.value = flattenCategory(tree)
        .filter((item) => !keyword || item.name.includes(keyword))
        .map((item) => ({
          id: item.id,
          name: item.name,
          image: item.extra || item.pic || item.image || '',
          url: `/pages/goods_cate/goods_cate?cid=${item.id}`
        }));
      linkTotal.value = linkRows.value.length;
    } else if (linkQuery.type === 'seckill') {
      const data = await seckillStoreList({ keywords: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_seckill_details/index');
      linkTotal.value = Number(data?.total || 0);
    } else if (linkQuery.type === 'combination') {
      const data = await combinationList({ keywords: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_combination_details/index');
      linkTotal.value = Number(data?.total || 0);
    } else if (linkQuery.type === 'bargain') {
      const data = await bargainList({ keywords: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = mapActivityLinkRows(data?.list || [], '/pages/activity/goods_bargain_details/index');
      linkTotal.value = Number(data?.total || 0);
    } else if (linkQuery.type === 'article') {
      const data = await articleList({ keywords: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.title || `文章${item.id}`,
        image: item.imageInput || item.image,
        url: `/pages/news/news_details/index?id=${item.id}`
      }));
      linkTotal.value = Number(data?.total || 0);
    } else {
      const data = await pageDiyList({ name: keyword, page: linkQuery.page, limit: linkQuery.limit });
      linkRows.value = (data?.list || []).map((item) => ({
        id: item.id,
        name: item.name || `微页面${item.id}`,
        url: `/pages/activity/small_page/index?id=${item.id}`
      }));
      linkTotal.value = Number(data?.total || 0);
    }
  } finally {
    linkLoading.value = false;
  }
}

function mapActivityLinkRows(list, path) {
  return list.map((item) => ({
    id: item.id,
    name: item.title || item.name || item.storeName || item.store_name || `活动${item.id}`,
    image: item.image,
    url: `${path}?id=${item.id}`
  }));
}

function selectLink(row) {
  if (linkApply.value) {
    linkApply.value(row);
  }
  linkApply.value = null;
  linkDialogVisible.value = false;
}

function defaultForm() {
  return {
    id: null,
    name: '',
    title: '首页',
    coverImage: '',
    templateName: '',
    value: '{}',
    status: 0,
    type: 0,
    isShow: 1,
    isBgColor: 0,
    isBgPic: 0,
    isDiy: 0,
    colorPicker: '',
    bgPic: '',
    bgTabVal: 0,
    returnAddress: '0',
    titleBgColor: '1',
    titleColor: '1',
    serviceStatus: 1,
    textPosition: 0
  };
}

function emptyImage(title) {
  return {
    uid: `image-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title,
    image: '',
    link: ''
  };
}

function emptyMenu(title, index) {
  const links = ['/pages/index/index', '/pages/goods_cate/goods_cate', '/pages/order_addcart/order_addcart', '/pages/user/index'];
  return {
    uid: `menu-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
    title,
    image: '',
    link: links[index] || ''
  };
}

function emptyFooterMenu(title, index) {
  const item = emptyMenu(title, index);
  return {
    ...item,
    activeImage: '',
    inactiveImage: ''
  };
}

function emptyNotice(title) {
  return {
    uid: `notice-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title,
    link: '/pages/news/news_list/index'
  };
}

function emptyHotWord(title) {
  return {
    uid: `hot-word-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title
  };
}

function emptyMerchant(title, index) {
  return {
    uid: `merchant-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
    id: '',
    title,
    subTitle: '品质好店',
    image: '',
    link: '/pages/goods/goods_list/index'
  };
}

function emptyHotspot(title) {
  return {
    uid: `hotspot-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title,
    link: '',
    x: 8,
    y: 8,
    width: 36,
    height: 22
  };
}

function hotspotImageItem(component) {
  const image = component.image || component.images?.[0]?.image || '';
  return {
    uid: component.images?.[0]?.uid || `hotspot-image-${Date.now()}`,
    title: component.title || '热区底图',
    image,
    link: component.link || ''
  };
}

function normalizeHotspots(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === 'object' ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    const parentWidth = Number(item.parentWidth || item.nowImgWidth || item.imgWidth || 390) || 390;
    const parentHeight = Number(item.parentHeight || item.imgHeight || 220) || 220;
    const fromPercent = item.x !== undefined || item.y !== undefined || item.width !== undefined || item.height !== undefined;
    return {
      uid: item.uid || `hotspot-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
      title: item.title || item.name || `热区${index + 1}`,
      link: item.link || item.url || item.value || '',
      x: clampPercent(fromPercent ? item.x : Number(item.starX || 0) / parentWidth * 100),
      y: clampPercent(fromPercent ? item.y : Number(item.starY || 0) / parentHeight * 100),
      width: clampPercent(fromPercent ? item.width : Number(item.areaWidth || 120) / parentWidth * 100, 1),
      height: clampPercent(fromPercent ? item.height : Number(item.areaHeight || 60) / parentHeight * 100, 1),
      starX: item.starX,
      starY: item.starY,
      areaWidth: item.areaWidth,
      areaHeight: item.areaHeight
    };
  });
}

function normalizeMerchants(value) {
  if (!value) {
    return [];
  }
  const list = Array.isArray(value) ? value : typeof value === 'object' ? Object.values(value) : [];
  return list.filter(Boolean).map((item, index) => {
    if (typeof item === 'string' || typeof item === 'number') {
      return {
        uid: `merchant-${Date.now()}-${index}`,
        id: item,
        title: `商户${item}`,
        subTitle: '品质好店',
        image: '',
        link: '/pages/goods/goods_list/index'
      };
    }
    return {
      uid: item.uid || `merchant-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
      id: item.id || item.merId || item.value || '',
      title: item.title || item.name || item.merName || `推荐店铺${index + 1}`,
      subTitle: item.subTitle || item.label || item.typeName || '品质好店',
      image: item.image || item.logo || item.avatar || item.pic || '',
      link: item.link || item.url || item.path || (item.id || item.merId ? `/pages/goods/goods_list/index?merId=${item.id || item.merId}` : '/pages/goods/goods_list/index')
    };
  });
}

function clampPercent(value, min = 0) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return min;
  }
  return Math.max(min, Math.min(100, Number(number.toFixed(1))));
}

function hotspotStyle(area) {
  return {
    left: `${clampPercent(area.x)}%`,
    top: `${clampPercent(area.y)}%`,
    width: `${clampPercent(area.width, 1)}%`,
    height: `${clampPercent(area.height, 1)}%`
  };
}

function componentLabel(type) {
  for (const group of componentGroups) {
    const found = group.list.find((item) => item.type === type);
    if (found) {
      return found.label;
    }
  }
  return type || '组件';
}

function normalizeDiyType(type) {
  const key = String(type || '').trim();
  const map = {
    homeComb: 'home_comb',
    homeHotspot: 'home_hotspot',
    headerSerch: 'search_box',
    homeTab: 'home_tab',
    pictureCube: 'picture_cube',
    pictureCure: 'picture_cube',
    swiperBg: 'banner',
    news: 'home_news_roll',
    footer: 'home_footer',
    tabNav: 'nav_bar',
    menus: 'home_menu',
    homeMenu: 'home_menu',
    titles: 'home_title',
    richTextEditor: 'z_ueditor',
    guide: 'z_auxiliary_line',
    blankPage: 'z_auxiliary_box',
    goodList: 'home_goods_list',
    homeCoupons: 'home_coupon',
    homeMerchant: 'home_merchant',
    merchant: 'home_merchant',
    homeArticle: 'home_article'
  };
  return map[key] || key;
}

function componentDefaults(type) {
  const map = {
    home_comb: { link: '/pages/goods/goods_search/index', limit: 4 },
    home_goods_list: { link: '/pages/goods/goods_list/index', limit: 4 },
    home_coupon: { link: '/pages/users/user_coupon/index', limit: 3 },
    home_merchant: { link: '/pages/goods/goods_list/index', limit: 3 },
    home_seckill: { link: '/pages/activity/goods_seckill/index', limit: 3 },
    home_group: { link: '/pages/activity/goods_combination/index', limit: 3 },
    home_bargain: { link: '/pages/activity/goods_bargain/index', limit: 3 },
    home_article: { link: '/pages/news/news_list/index', limit: 4 }
  };
  return map[type] || { link: '', limit: 4 };
}

function staticLinks() {
  return [
    { name: '商城首页', url: '/pages/index/index' },
    { name: '商品分类', url: '/pages/goods_cate/goods_cate' },
    { name: '购物车', url: '/pages/order_addcart/order_addcart' },
    { name: '个人中心', url: '/pages/user/index' },
    { name: '我的订单', url: '/pages/order/list' },
    { name: '售后/退款', url: '/pages/users/user_return_list/index' },
    { name: '我的推广', url: '/pages/promoter/index' },
    { name: '优惠券', url: '/pages/users/user_coupon/index' },
    { name: '余额', url: '/pages/users/user_money/index' },
    { name: '签到', url: '/pages/users/user_sgin/index' },
    { name: '秒杀列表', url: '/pages/activity/goods_seckill/index' },
    { name: '拼团列表', url: '/pages/activity/goods_combination/index' },
    { name: '砍价列表', url: '/pages/activity/goods_bargain/index' }
  ];
}

function flattenCategory(list, result = []) {
  for (const item of list || []) {
    result.push(item);
    flattenCategory(item.child || item.children || [], result);
  }
  return result;
}

function titleBgColor(value) {
  if (!value || value === '1') {
    return '#ffffff';
  }
  if (value === '0') {
    return '#f5f5f5';
  }
  return value;
}

function titleColor(value) {
  if (!value || value === '1') {
    return '#333333';
  }
  if (value === '0') {
    return '#ffffff';
  }
  return value;
}

function compactParams(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function numberOr(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function dateText(value) {
  return value ? String(value).replace('T', ' ') : '-';
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}
</script>

<style scoped>
.diy-shell {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  min-height: 620px;
}

.tab-view {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #eee;
  margin-right: 40px;
}

.tab-view button {
  height: 50px;
  border: 0;
  padding-left: 30px;
  background: transparent;
  color: #303133;
  text-align: left;
  cursor: pointer;
}

.tab-view button.active {
  background: #ecf5ff;
  color: #409eff;
  border-right: 2px solid #409eff;
}

.home-panel {
  display: grid;
  grid-template-columns: 390px minmax(320px, 1fr);
  gap: 40px;
  align-items: start;
}

.phone-preview {
  position: relative;
  width: 390px;
  height: 650px;
  border: 1px solid #eeeeee;
  background: #f5f7fa;
}

.phone-preview iframe,
.preview-frame {
  width: 100%;
  height: 100%;
  border: 0;
}

.preview-mask {
  position: absolute;
  inset: 0;
}

.home-actions {
  position: relative;
  padding: 20px;
  border: 1px solid #eeeeee;
}

.home-actions::before {
  content: "";
  position: absolute;
  left: -10px;
  top: 23px;
  border-right: 10px solid #eeeeee;
  border-top: 10px solid transparent;
  border-bottom: 10px solid transparent;
}

.qr-card {
  margin-top: 20px;
  background: #f9f9f9;
}

.qr-card :deep(.el-card__body) {
  display: flex;
  min-height: 120px;
  align-items: center;
  justify-content: space-between;
}

.qr-title {
  color: #303133;
  font-size: 20px;
  font-weight: 700;
}

.qr-tip {
  margin-top: 18px;
  color: #909399;
  font-size: 14px;
  word-break: break-all;
}

.qr-placeholder {
  display: grid;
  width: 120px;
  height: 120px;
  place-items: center;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #909399;
  font-weight: 700;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.selWidth {
  width: 300px;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cover-image {
  width: 46px;
  height: 46px;
  border-radius: 4px;
}

.image-field {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto auto;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.form-image-preview,
.form-image-empty {
  width: 52px;
  height: 52px;
  border-radius: 4px;
  background: #f5f7fa;
}

.form-image-empty {
  display: grid;
  place-items: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
  font-size: 12px;
}

.attachment-picker {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  min-height: 430px;
}

.attachment-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  overflow: auto;
}

.attachment-main {
  min-width: 0;
}

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(116px, 1fr));
  gap: 12px;
  min-height: 360px;
}

.attachment-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  padding: 8px;
  text-align: left;
}

.attachment-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.attachment-item :deep(.el-image) {
  width: 100%;
  height: 88px;
  display: block;
  background: #f5f7fa;
}

.attachment-item span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: #606266;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #909399;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.attachment-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.block {
  padding-bottom: 20px;
}

.preview-frame {
  width: 390px;
  height: 650px;
}

.page-diy-designer-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f0f2f5;
}

.designer-head {
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.designer-head strong {
  display: block;
  color: #303133;
  font-size: 16px;
}

.designer-head span {
  color: #909399;
  font-size: 12px;
}

.designer-actions {
  display: flex;
  gap: 8px;
}

.designer-layout {
  display: grid;
  grid-template-columns: 220px minmax(420px, 1fr) 390px;
  height: calc(100vh - 66px);
  min-height: 680px;
}

.designer-palette,
.designer-config {
  overflow: auto;
  background: #fff;
}

.designer-palette {
  border-right: 1px solid #ebeef5;
  padding-bottom: 20px;
}

.palette-title {
  height: 42px;
  line-height: 42px;
  padding: 0 18px;
  color: #606266;
  font-weight: 700;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.palette-item {
  width: 86px;
  height: 76px;
  margin: 14px 0 0 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  cursor: pointer;
}

.palette-item:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.palette-item span,
.palette-item em {
  display: block;
  font-style: normal;
}

.palette-item span {
  font-weight: 700;
  margin-bottom: 8px;
}

.designer-phone-wrap {
  overflow: auto;
  padding: 28px 0 60px;
}

.designer-phone {
  width: 390px;
  min-height: 710px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
}

.phone-status {
  height: 28px;
  background: #111;
}

.phone-title {
  height: 42px;
  line-height: 42px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.phone-title.active,
.phone-component.active {
  outline: 2px solid #409eff;
}

.phone-body {
  min-height: 640px;
  padding-bottom: 80px;
  background-repeat: repeat;
  background-size: cover;
}

.phone-empty {
  margin: 16px;
  padding: 40px 0;
  text-align: center;
  color: #909399;
  border: 1px dashed #dcdfe6;
  background: rgba(255, 255, 255, 0.7);
}

.phone-component {
  position: relative;
  min-height: 42px;
  background: #fff;
  cursor: pointer;
}

.phone-component.hidden {
  opacity: 0.45;
}

.component-name {
  position: absolute;
  left: -76px;
  top: 8px;
  min-width: 64px;
  padding: 2px 6px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 3px;
  font-size: 12px;
  text-align: center;
}

.component-toolbar {
  display: none;
  position: absolute;
  right: -230px;
  top: 0;
  z-index: 3;
  gap: 4px;
}

.phone-component.active .component-toolbar {
  display: flex;
}

.component-toolbar button {
  border: 0;
  padding: 4px 6px;
  border-radius: 3px;
  background: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.component-toolbar button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.designer-config {
  border-left: 1px solid #ebeef5;
  padding: 18px;
}

.designer-config h3 {
  margin: 0 0 16px;
  font-size: 16px;
}

.config-inline-input {
  width: 140px;
  margin-left: 8px;
}

.config-image-field {
  grid-template-columns: 52px minmax(0, 1fr) auto;
}

.config-list {
  width: 100%;
}

.config-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.config-row .el-input:nth-of-type(n + 2) {
  grid-column: 1 / 5;
}

.footer-config-row {
  grid-template-columns: 58px minmax(0, 1fr) auto;
}

.footer-config-row .el-input:nth-of-type(n + 2) {
  grid-column: auto;
}

.config-thumb {
  width: 42px;
  height: 42px;
  background: #f5f7fa;
}

.config-thumb-empty {
  display: grid;
  place-items: center;
  color: #909399;
  border: 1px solid #ebeef5;
  font-size: 12px;
}

.merchant-config-row {
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
}

.merchant-config-row .el-input:nth-of-type(2) {
  grid-column: 1 / 3;
}

.merchant-config-row .el-input:nth-of-type(3) {
  grid-column: 3 / 5;
}

.mt8 {
  margin-top: 8px;
}

.preview-search {
  margin: 10px 12px;
  height: 34px;
  line-height: 34px;
  padding-left: 15px;
  border-radius: 17px;
  background: #f5f5f5;
  color: #999;
}

.preview-banner {
  height: 150px;
  display: grid;
  place-items: center;
  background: #f5f7fa;
  color: #909399;
}

.preview-banner img,
.preview-picture-cube img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 0;
  padding: 14px 8px;
}

.preview-menu-item {
  text-align: center;
  color: #606266;
  font-size: 12px;
}

.preview-menu-item img,
.preview-menu-item i {
  display: grid;
  width: 34px;
  height: 34px;
  margin: 0 auto 6px;
  place-items: center;
  border-radius: 50%;
  background: #f0f2f5;
  font-style: normal;
}

.preview-nav-bar {
  display: flex;
  gap: 14px;
  overflow: hidden;
  padding: 12px;
  background: #fff;
  color: #303133;
  font-size: 13px;
  white-space: nowrap;
}

.preview-nav-bar span {
  position: relative;
  flex: 0 0 auto;
  padding-bottom: 6px;
}

.preview-nav-bar span.active {
  color: #e93323;
  font-weight: 700;
}

.preview-nav-bar span.active::after {
  position: absolute;
  right: 20%;
  bottom: 0;
  left: 20%;
  height: 2px;
  border-radius: 2px;
  background: #e93323;
  content: "";
}

.preview-picture-cube {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 8px;
}

.preview-picture-cube div {
  height: 92px;
  display: grid;
  place-items: center;
  background: #f5f7fa;
  color: #909399;
}

.preview-news-roll {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 10px;
  padding: 10px 12px;
  border-radius: 4px;
  background: #fff;
  color: #303133;
  font-size: 13px;
}

.preview-news-roll img {
  width: 42px;
  height: 18px;
  object-fit: contain;
}

.preview-news-roll strong {
  flex: 0 0 auto;
  color: #e93323;
  font-size: 13px;
}

.preview-news-roll span {
  overflow: hidden;
  min-width: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-footer-nav {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  padding: 10px 6px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.preview-footer-nav div {
  display: grid;
  place-items: center;
  gap: 4px;
  color: #606266;
  font-size: 12px;
}

.preview-footer-nav div.active {
  color: #e93323;
}

.preview-footer-nav img,
.preview-footer-nav i {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  background: #f5f7fa;
  font-style: normal;
  object-fit: contain;
}

.preview-hotspot,
.hotspot-preview {
  position: relative;
  overflow: hidden;
  min-height: 150px;
  background: #f5f7fa;
}

.preview-hotspot img,
.hotspot-preview img {
  display: block;
  width: 100%;
  min-height: 150px;
  object-fit: cover;
}

.preview-hotspot-empty,
.hotspot-empty {
  display: grid;
  min-height: 150px;
  place-items: center;
  color: #909399;
}

.preview-hotspot-area,
.hotspot-area {
  position: absolute;
  display: grid;
  place-items: center;
  border: 1px solid rgba(64, 158, 255, 0.9);
  border-radius: 3px;
  background: rgba(64, 158, 255, 0.18);
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.hotspot-area {
  cursor: pointer;
}

.hotspot-area.active {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.22);
  color: #f56c6c;
}

.hotspot-editor {
  width: 100%;
}

.hotspot-list {
  margin-top: 10px;
}

.hotspot-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) minmax(0, 1.4fr) auto;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.hotspot-row :deep(.el-input-number) {
  width: 100%;
}

.preview-rich {
  padding: 12px;
  color: #303133;
  line-height: 1.6;
}

.preview-title,
.preview-section-title {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  font-size: 15px;
}

.preview-line {
  height: 1px;
  margin: 12px;
  background: #e5e5e5;
}

.preview-space {
  height: 28px;
}

.preview-video {
  height: 180px;
  display: grid;
  place-items: center;
  margin: 10px 12px;
  background: #1f2329;
  color: #fff;
}

.preview-goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 0 8px 12px;
}

.preview-goods-card {
  min-width: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.preview-goods-img {
  height: 110px;
  display: grid;
  place-items: center;
  background: #f5f7fa;
  color: #909399;
}

.preview-goods-card strong,
.preview-goods-card span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-goods-card span {
  color: #e93323;
}

.preview-coupon {
  padding-bottom: 12px;
}

.preview-coupon-row {
  display: flex;
  gap: 8px;
  padding: 0 10px;
  overflow: hidden;
}

.preview-coupon-card {
  flex: 0 0 108px;
  min-height: 76px;
  padding: 10px;
  border-radius: 4px;
  background: linear-gradient(135deg, #fff1ed, #fff);
  border: 1px solid #ffd8ce;
  color: #e93323;
}

.preview-coupon-card strong,
.preview-coupon-card span,
.preview-coupon-card em {
  display: block;
}

.preview-coupon-card span {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.preview-coupon-card em {
  margin-top: 8px;
  font-style: normal;
  font-size: 12px;
}

.preview-merchant {
  padding-bottom: 12px;
}

.preview-merchant-row {
  display: flex;
  gap: 8px;
  overflow: hidden;
  padding: 0 10px;
}

.preview-merchant-card {
  flex: 0 0 108px;
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 10px 8px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fff;
}

.preview-merchant-card img,
.preview-merchant-card i {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  background: #fff0ed;
  color: #e93323;
  font-style: normal;
  font-weight: 700;
  object-fit: cover;
}

.preview-merchant-card strong,
.preview-merchant-card span {
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-merchant-card strong {
  color: #303133;
  font-size: 12px;
}

.preview-merchant-card span {
  color: #e93323;
  font-size: 11px;
}

.preview-activity {
  padding-bottom: 12px;
}

.preview-activity-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  font-size: 15px;
}

.preview-activity-head span {
  color: #909399;
  font-size: 12px;
}

.preview-activity-row {
  display: flex;
  gap: 8px;
  padding: 0 10px;
  overflow: hidden;
}

.preview-activity-card {
  flex: 0 0 112px;
  min-width: 0;
  padding: 8px;
  border-radius: 4px;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.preview-activity-img {
  height: 82px;
  display: grid;
  place-items: center;
  background: #f5f7fa;
  color: #909399;
}

.preview-activity-card strong,
.preview-activity-card span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-activity-card span {
  color: #e93323;
  font-size: 12px;
}

.preview-article-list {
  padding-bottom: 8px;
}

.preview-article-item {
  margin: 0 12px 8px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.preview-article-item strong,
.preview-article-item span {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-article-item span {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.link-picker {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  min-height: 500px;
}

.link-picker aside {
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
}

.link-picker aside button {
  display: block;
  width: 100%;
  height: 38px;
  border: 0;
  background: transparent;
  text-align: left;
  padding-left: 12px;
  cursor: pointer;
}

.link-picker aside button.active {
  color: #409eff;
  background: #ecf5ff;
}

.link-picker section {
  min-width: 0;
  padding-left: 16px;
}

.link-search {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.link-row-image {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  background: #f5f7fa;
}

.link-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .diy-shell,
  .home-panel,
  .designer-layout {
    grid-template-columns: 1fr;
  }

  .tab-view {
    flex-direction: row;
    margin-right: 0;
    margin-bottom: 20px;
    border-right: 0;
    border-bottom: 1px solid #eee;
  }
}
</style>

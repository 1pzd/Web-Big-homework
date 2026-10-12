/* =====================================================================
   后台公共脚本：侧边栏与顶栏渲染、管理员身份校验、通用分页组件
   ===================================================================== */

const AdminLayout = (function () {

    const MENU = [
        { key: 'dashboard', href: '/admin', icon: '◧', label: '数据看板' },
        { key: 'knowledge', href: '/admin/knowledge', icon: '▤', label: '知识库管理' },
        { key: 'category', href: '/admin/category', icon: '▩', label: '分类管理' },
        { key: 'user', href: '/admin/user', icon: '◍', label: '用户管理' },
        { key: 'message', href: '/admin/message', icon: '◷', label: '问答记录' },
        { key: 'config', href: '/admin/config', icon: '⚙', label: '系统配置' }
    ];

    /**
     * 初始化后台页面。
     * @param {string} activeKey 当前菜单标识
     * @param {string} title 顶栏标题
     * @returns {Promise<object|null>} 当前管理员信息
     */
    async function init(activeKey, title) {
        const user = await App.currentUser();
        if (!user) {
            location.href = '/admin/login';
            return null;
        }
        if (user.role !== 'ADMIN') {
            alert('当前账号不是管理员，无法访问后台');
            location.href = '/';
            return null;
        }

        renderSidebar(activeKey);
        renderTopbar(title, user);
        App.bindLogout('[data-logout]');
        return user;
    }

    function renderSidebar(activeKey) {
        const mount = document.getElementById('sidebarMount');
        if (!mount) return;
        const items = MENU.map(function (m) {
            const cls = m.key === activeKey ? 'menu-item active' : 'menu-item';
            return '<a class="' + cls + '" href="' + m.href + '">'
                + '<span class="ico">' + m.icon + '</span>' + m.label + '</a>';
        }).join('');
        mount.outerHTML = '<aside class="sidebar" id="sidebar">'
            + '<div class="sidebar-brand"><span class="nav-logo">智</span>智问 · 管理后台</div>'
            + '<nav class="sidebar-menu"><div class="menu-group">内容运营</div>' + items + '</nav>'
            + '<div class="sidebar-foot">学号 20232653 · 物联网 2301<br>Java Web 课程设计</div>'
            + '</aside>';
    }

    function renderTopbar(title, user) {
        const mount = document.getElementById('topbarMount');
        if (!mount) return;
        const name = user.nickname || user.username;
        mount.outerHTML = '<header class="topbar" id="topbar">'
            + '<h1>' + App.esc(title) + '</h1>'
            + '<span class="crumb">后台管理 / ' + App.esc(title) + '</span>'
            + '<div class="topbar-right">'
            + '<a class="nav-user" href="/" target="_blank"><span class="avatar">'
            + App.esc(name.substring(0, 1)) + '</span>' + App.esc(name) + '</a>'
            + '<button class="btn btn-ghost btn-sm" data-logout>退出</button>'
            + '</div></header>';
    }

    return { init: init };
})();

/**
 * 简易分页渲染器。
 * @param {HTMLElement} mount 容器
 * @param {object} page 分页数据 {current,size,total,pages}
 * @param {function} onChange 页码变化回调
 */
function renderPager(mount, page, onChange) {
    const current = Number(page.current || 1);
    const pages = Number(page.pages || 0);
    const total = Number(page.total || 0);
    if (!mount) return;

    if (total === 0) {
        mount.innerHTML = '<span class="info">共 0 条记录</span>';
        return;
    }

    const buttons = [];
    buttons.push('<button data-p="' + (current - 1) + '"' + (current <= 1 ? ' disabled' : '') + '>上一页</button>');

    const start = Math.max(1, current - 2);
    const end = Math.min(pages, start + 4);
    for (let i = start; i <= end; i++) {
        buttons.push('<button data-p="' + i + '"' + (i === current ? ' class="cur"' : '') + '>' + i + '</button>');
    }
    buttons.push('<button data-p="' + (current + 1) + '"' + (current >= pages ? ' disabled' : '') + '>下一页</button>');

    mount.innerHTML = '<span class="info">共 ' + total + ' 条 · 第 ' + current + ' / ' + pages + ' 页</span>'
        + '<span class="spacer"></span>' + buttons.join('');

    mount.querySelectorAll('button[data-p]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const target = Number(btn.dataset.p);
            if (target >= 1 && target <= pages && target !== current) {
                onChange(target);
            }
        });
    });
}
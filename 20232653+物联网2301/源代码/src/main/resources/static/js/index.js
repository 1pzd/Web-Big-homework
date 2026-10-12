/* 首页：加载站点信息、平台规模数据与推荐问题 */
(function () {
    App.api('/api/public/site').then(function (site) {
        if (site && site.name) {
            document.getElementById('siteName').textContent = site.name;
            document.title = site.name;
        }
        if (site && site.notice) {
            document.getElementById('siteNotice').textContent = site.notice;
        }
    }).catch(function () { /* 首页信息加载失败不影响主流程 */ });

    App.api('/api/public/overview').then(function (data) {
        document.getElementById('statKnowledge').textContent = data.knowledgeCount || 0;
        document.getElementById('statUser').textContent = data.userCount || 0;
        const rate = data.hitRate === null || data.hitRate === undefined ? 0 : data.hitRate;
        document.getElementById('statHit').textContent = Math.round(rate * 100) + '%';
    }).catch(function () {
        document.getElementById('statKnowledge').textContent = '0';
        document.getElementById('statUser').textContent = '0';
        document.getElementById('statHit').textContent = '0%';
    });

    App.api('/api/public/hot-questions?limit=8').then(function (list) {
        const box = document.getElementById('hotQuestions');
        if (!list || list.length === 0) {
            box.innerHTML = '<span class="chip">暂无推荐问题</span>';
            return;
        }
        box.innerHTML = list.map(function (q) {
            return '<a class="chip" href="/chat?q=' + encodeURIComponent(q) + '">' + App.esc(q) + '</a>';
        }).join('');
    }).catch(function () {
        document.getElementById('hotQuestions').innerHTML = '<span class="chip">暂无推荐问题</span>';
    });

    // 已登录时把右上角换成用户信息
    App.currentUser().then(function (user) {
        if (!user) return;
        const box = document.getElementById('navRight');
        const name = user.nickname || user.username;
        let html = '<a class="nav-user" href="/history"><span class="avatar">'
            + App.esc(name.substring(0, 1)) + '</span>' + App.esc(name) + '</a>';
        if (user.role === 'ADMIN') {
            html += '<a class="btn btn-ghost btn-sm" href="/admin">管理后台</a>';
        }
        html += '<button class="btn btn-ghost btn-sm" data-logout>退出</button>';
        box.innerHTML = html;
        App.bindLogout('[data-logout]');
    });
})();
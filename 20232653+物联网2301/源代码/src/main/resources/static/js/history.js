/* 问答历史页：会话列表、展开对话详情、删除与清空 */
(function () {
    const listBox = document.getElementById('histList');
    const subBox = document.getElementById('histSub');

    function sessionHtml(s) {
        return '<div class="hist-card" data-id="' + s.id + '">'
            + '<div class="hist-head">'
            + '<span class="avatar" style="background:#eef0ff;color:#5b5bf5;">问</span>'
            + '<span class="t">' + App.esc(s.title || '未命名会话') + '</span>'
            + '<span class="time">' + s.messageCount + ' 条 · ' + App.formatTime(s.lastTime) + '</span>'
            + '<button class="btn btn-ghost btn-sm" data-toggle>展开</button>'
            + '<button class="btn btn-ghost btn-sm" data-del>删除</button>'
            + '</div>'
            + '<div class="hist-msgs"></div>'
            + '</div>';
    }

    async function load() {
        try {
            const page = await App.api('/api/chat/sessions?current=1&size=50');
            const records = page.records || [];
            subBox.textContent = '共 ' + page.total + ' 个会话';
            if (records.length === 0) {
                listBox.innerHTML = '<div class="empty"><div class="empty-icon">◷</div>'
                    + '<div>还没有问答记录，去<a href="/chat" style="color:#5b5bf5;">问一个问题</a>吧</div></div>';
                return;
            }
            listBox.innerHTML = records.map(sessionHtml).join('');
        } catch (e) {
            listBox.innerHTML = '<div class="empty">加载失败：' + App.esc(e.message) + '</div>';
        }
    }

    async function toggle(card, button) {
        const box = card.querySelector('.hist-msgs');
        if (box.classList.contains('open')) {
            box.classList.remove('open');
            button.textContent = '展开';
            return;
        }
        if (!box.dataset.loaded) {
            button.textContent = '加载中…';
            try {
                const messages = await App.api('/api/chat/messages/' + card.dataset.id);
                box.innerHTML = messages.map(function (m) {
                    const isUser = m.role === 'user';
                    return '<div class="hist-msg ' + (isUser ? 'user' : '') + '">'
                        + '<span class="r">' + (isUser ? '我' : '智问') + '：</span>'
                        + App.esc(m.content)
                        + (isUser ? '' : ' ' + App.sourceTag(m.source))
                        + '</div>';
                }).join('');
                box.dataset.loaded = '1';
            } catch (e) {
                App.toast(e.message, 'error');
                button.textContent = '展开';
                return;
            }
        }
        box.classList.add('open');
        button.textContent = '收起';
    }

    listBox.addEventListener('click', async function (e) {
        const card = e.target.closest('.hist-card');
        if (!card) return;

        if (e.target.closest('[data-toggle]')) {
            toggle(card, e.target.closest('[data-toggle]'));
            return;
        }

        if (e.target.closest('[data-del]')) {
            if (!confirm('确定删除这个会话吗？删除后不可恢复。')) return;
            try {
                await App.api('/api/chat/sessions/' + card.dataset.id, { method: 'DELETE' });
                App.toast('会话已删除', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
        }
    });

    document.getElementById('clearBtn').addEventListener('click', async function () {
        if (!confirm('确定清空全部问答历史吗？该操作不可恢复。')) return;
        try {
            await App.api('/api/chat/sessions', { method: 'DELETE' });
            App.toast('已清空全部会话', 'success');
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    });

    (async function init() {
        const user = await App.currentUser();
        if (user) {
            const name = user.nickname || user.username;
            document.getElementById('userName').textContent = name;
            document.getElementById('userAvatar').textContent = name.substring(0, 1);
        }
        App.bindLogout('[data-logout]');
        load();
    })();
})();
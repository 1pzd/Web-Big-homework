/* 后台问答记录：会话列表、查看对话详情、删除会话 */
(function () {
    const state = { current: 1, size: 10, keyword: '' };
    const body = document.getElementById('tableBody');
    const modal = document.getElementById('modal');
    const modalBody = document.getElementById('modalBody');

    async function load() {
        const params = new URLSearchParams({ current: state.current, size: state.size });
        if (state.keyword) params.set('keyword', state.keyword);

        body.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#8a90a8;padding:24px;">加载中…</td></tr>';
        try {
            const page = await App.api('/api/admin/session/page?' + params.toString());
            const records = page.records || [];
            body.innerHTML = records.length === 0
                ? '<tr><td colspan="6" style="text-align:center;color:#8a90a8;padding:32px;">暂无问答记录</td></tr>'
                : records.map(function (s) {
                    return '<tr>'
                        + '<td>' + s.id + '</td>'
                        + '<td class="cell-ellipsis">' + App.esc(s.title || '未命名会话') + '</td>'
                        + '<td>' + s.userId + '</td>'
                        + '<td>' + s.messageCount + '</td>'
                        + '<td>' + App.formatTime(s.lastTime) + '</td>'
                        + '<td class="actions">'
                        + '<button class="btn btn-ghost btn-sm" data-view="' + s.id + '" data-title="'
                        + App.esc(s.title || '') + '">查看</button> '
                        + '<button class="btn btn-ghost btn-sm" data-del="' + s.id + '">删除</button>'
                        + '</td></tr>';
                }).join('');
            renderPager(document.getElementById('pager'), page, function (p) {
                state.current = p;
                load();
            });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#e11d48;padding:24px;">'
                + App.esc(e.message) + '</td></tr>';
        }
    }

    async function viewSession(id, title) {
        document.getElementById('modalTitle').textContent = title || '对话详情';
        modalBody.innerHTML = '<div style="color:#8a90a8;text-align:center;padding:20px;">加载中…</div>';
        modal.classList.add('open');
        try {
            const messages = await App.api('/api/admin/session/' + id + '/messages');
            if (!messages || messages.length === 0) {
                modalBody.innerHTML = '<div style="color:#8a90a8;text-align:center;padding:20px;">该会话没有消息</div>';
                return;
            }
            modalBody.innerHTML = messages.map(function (m) {
                const isUser = m.role === 'user';
                const meta = isUser ? '' : '<div style="margin-top:6px;font-size:12px;color:#8a90a8;">'
                    + App.sourceTag(m.source)
                    + (m.score === null || m.score === undefined ? '' : ' 相似度 ' + m.score)
                    + (m.costMs === null || m.costMs === undefined ? '' : ' · 耗时 ' + m.costMs + ' ms')
                    + '</div>';
                return '<div class="dialog-item ' + (isUser ? 'user' : '') + '">'
                    + '<div class="role">' + (isUser ? '用户' : '智问') + '</div>'
                    + '<div class="text">' + App.esc(m.content) + meta + '</div>'
                    + '</div>';
            }).join('');
        } catch (e) {
            modalBody.innerHTML = '<div style="color:#e11d48;text-align:center;padding:20px;">'
                + App.esc(e.message) + '</div>';
        }
    }

    document.getElementById('searchBtn').addEventListener('click', function () {
        state.current = 1;
        state.keyword = document.getElementById('keyword').value.trim();
        load();
    });

    document.getElementById('keyword').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') document.getElementById('searchBtn').click();
    });

    document.getElementById('modalClose').addEventListener('click', function () {
        modal.classList.remove('open');
    });
    document.getElementById('closeBtn').addEventListener('click', function () {
        modal.classList.remove('open');
    });
    modal.addEventListener('click', function (e) {
        if (e.target === modal) modal.classList.remove('open');
    });

    body.addEventListener('click', async function (e) {
        const viewBtn = e.target.closest('[data-view]');
        if (viewBtn) {
            viewSession(viewBtn.dataset.view, viewBtn.dataset.title);
            return;
        }

        const delBtn = e.target.closest('[data-del]');
        if (delBtn) {
            if (!confirm('确定删除该会话及其全部消息吗？')) return;
            try {
                await App.api('/api/admin/session/' + delBtn.dataset.del, { method: 'DELETE' });
                App.toast('会话已删除', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
        }
    });

    (async function init() {
        const user = await AdminLayout.init('message', '问答记录');
        if (!user) return;
        load();
    })();
})();
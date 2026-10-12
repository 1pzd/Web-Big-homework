/* 后台用户管理：分页查询、启停、重置密码、删除 */
(function () {
    const state = { current: 1, size: 10, keyword: '', role: '' };
    const body = document.getElementById('tableBody');
    const modal = document.getElementById('modal');

    async function load() {
        const params = new URLSearchParams({ current: state.current, size: state.size });
        if (state.keyword) params.set('keyword', state.keyword);
        if (state.role) params.set('role', state.role);

        body.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#8a90a8;padding:24px;">加载中…</td></tr>';
        try {
            const page = await App.api('/api/admin/user/page?' + params.toString());
            const records = page.records || [];
            body.innerHTML = records.length === 0
                ? '<tr><td colspan="8" style="text-align:center;color:#8a90a8;padding:32px;">没有符合条件的用户</td></tr>'
                : records.map(function (u) {
                    const roleBadge = u.role === 'ADMIN'
                        ? '<span class="badge badge-admin">管理员</span>'
                        : '<span class="badge badge-user">普通用户</span>';
                    const statusBadge = u.status === 1
                        ? '<span class="badge badge-on">正常</span>'
                        : '<span class="badge badge-danger">已禁用</span>';
                    return '<tr>'
                        + '<td>' + u.id + '</td>'
                        + '<td>' + App.esc(u.username) + '</td>'
                        + '<td>' + App.esc(u.nickname || '-') + '</td>'
                        + '<td>' + App.esc(u.email || '-') + '</td>'
                        + '<td>' + roleBadge + '</td>'
                        + '<td>' + statusBadge + '</td>'
                        + '<td>' + App.formatTime(u.createTime) + '</td>'
                        + '<td class="actions">'
                        + '<button class="btn btn-ghost btn-sm" data-status="' + u.id + '" data-cur="' + u.status + '">'
                        + (u.status === 1 ? '禁用' : '启用') + '</button> '
                        + '<button class="btn btn-ghost btn-sm" data-pwd="' + u.id + '">重置密码</button> '
                        + '<button class="btn btn-ghost btn-sm" data-del="' + u.id + '">删除</button>'
                        + '</td></tr>';
                }).join('');
            renderPager(document.getElementById('pager'), page, function (p) {
                state.current = p;
                load();
            });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#e11d48;padding:24px;">'
                + App.esc(e.message) + '</td></tr>';
        }
    }

    document.getElementById('searchBtn').addEventListener('click', function () {
        state.current = 1;
        state.keyword = document.getElementById('keyword').value.trim();
        state.role = document.getElementById('roleFilter').value;
        load();
    });

    document.getElementById('keyword').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') document.getElementById('searchBtn').click();
    });

    document.getElementById('modalClose').addEventListener('click', function () {
        modal.classList.remove('open');
    });
    document.getElementById('cancelBtn').addEventListener('click', function () {
        modal.classList.remove('open');
    });
    modal.addEventListener('click', function (e) {
        if (e.target === modal) modal.classList.remove('open');
    });

    document.getElementById('saveBtn').addEventListener('click', async function () {
        const id = document.getElementById('formId').value;
        const password = document.getElementById('formPassword').value;
        if (password.length < 6 || password.length > 32) {
            App.toast('密码长度需为 6~32 位', 'error');
            return;
        }
        try {
            await App.api('/api/admin/user/' + id + '/password?password=' + encodeURIComponent(password),
                { method: 'PUT' });
            App.toast('密码已重置', 'success');
            modal.classList.remove('open');
        } catch (e) {
            App.toast(e.message, 'error');
        }
    });

    body.addEventListener('click', async function (e) {
        const statusBtn = e.target.closest('[data-status]');
        if (statusBtn) {
            const next = Number(statusBtn.dataset.cur) === 1 ? 0 : 1;
            try {
                await App.api('/api/admin/user/' + statusBtn.dataset.status + '/status?status=' + next,
                    { method: 'PUT' });
                App.toast('状态已更新', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
            return;
        }

        const pwdBtn = e.target.closest('[data-pwd]');
        if (pwdBtn) {
            document.getElementById('formId').value = pwdBtn.dataset.pwd;
            document.getElementById('formPassword').value = '';
            modal.classList.add('open');
            return;
        }

        const delBtn = e.target.closest('[data-del]');
        if (delBtn) {
            if (!confirm('确定删除该用户吗？')) return;
            try {
                await App.api('/api/admin/user/' + delBtn.dataset.del, { method: 'DELETE' });
                App.toast('用户已删除', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
        }
    });

    (async function init() {
        const user = await AdminLayout.init('user', '用户管理');
        if (!user) return;
        load();
    })();
})();
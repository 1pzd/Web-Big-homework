/* 后台知识库管理：列表、筛选、新增/编辑、删除、启停 */
(function () {

    const state = { current: 1, size: 10, keyword: '', categoryId: '', status: '' };
    const body = document.getElementById('tableBody');
    const modal = document.getElementById('modal');

    async function loadCategories() {
        const list = await App.api('/api/admin/category/options');
        const options = '<option value="">全部分类</option>' + list.map(function (c) {
            return '<option value="' + c.id + '">' + App.esc(c.name) + '</option>';
        }).join('');
        document.getElementById('categoryFilter').innerHTML = options;
        document.getElementById('formCategory').innerHTML =
            '<option value="">未分类</option>' + list.map(function (c) {
                return '<option value="' + c.id + '">' + App.esc(c.name) + '</option>';
            }).join('');
    }

    async function load() {
        const params = new URLSearchParams({
            current: state.current,
            size: state.size
        });
        if (state.keyword) params.set('keyword', state.keyword);
        if (state.categoryId) params.set('categoryId', state.categoryId);
        if (state.status !== '') params.set('status', state.status);

        body.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#8a90a8;padding:24px;">加载中…</td></tr>';
        try {
            const page = await App.api('/api/admin/knowledge/page?' + params.toString());
            const records = page.records || [];
            body.innerHTML = records.length === 0
                ? '<tr><td colspan="8" style="text-align:center;color:#8a90a8;padding:32px;">没有符合条件的知识条目</td></tr>'
                : records.map(function (k) {
                    const statusBadge = k.status === 1
                        ? '<span class="badge badge-on">已启用</span>'
                        : '<span class="badge badge-off">已停用</span>';
                    return '<tr>'
                        + '<td>' + k.id + '</td>'
                        + '<td class="cell-ellipsis" title="' + App.esc(k.question) + '">' + App.esc(k.question) + '</td>'
                        + '<td>' + App.esc(k.categoryName || '未分类') + '</td>'
                        + '<td class="cell-ellipsis" title="' + App.esc(k.keywords) + '">' + App.esc(k.keywords || '-') + '</td>'
                        + '<td>' + k.hitCount + '</td>'
                        + '<td>' + statusBadge + '</td>'
                        + '<td>' + App.formatTime(k.updateTime) + '</td>'
                        + '<td class="actions">'
                        + '<button class="btn btn-ghost btn-sm" data-edit="' + k.id + '">编辑</button> '
                        + '<button class="btn btn-ghost btn-sm" data-status="' + k.id + '" data-cur="' + k.status + '">'
                        + (k.status === 1 ? '停用' : '启用') + '</button> '
                        + '<button class="btn btn-ghost btn-sm" data-del="' + k.id + '">删除</button>'
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

    function openModal(item) {
        document.getElementById('modalTitle').textContent = item ? '编辑知识条目' : '新增知识条目';
        document.getElementById('formId').value = item ? item.id : '';
        document.getElementById('formCategory').value = item && item.categoryId ? item.categoryId : '';
        document.getElementById('formStatus').value = item ? item.status : 1;
        document.getElementById('formQuestion').value = item ? item.question : '';
        document.getElementById('formKeywords').value = item ? (item.keywords || '') : '';
        document.getElementById('formAnswer').value = item ? item.answer : '';
        modal.classList.add('open');
    }

    function closeModal() {
        modal.classList.remove('open');
    }

    async function save() {
        const id = document.getElementById('formId').value;
        const categoryId = document.getElementById('formCategory').value;
        const payload = {
            id: id ? Number(id) : null,
            categoryId: categoryId ? Number(categoryId) : null,
            question: document.getElementById('formQuestion').value.trim(),
            answer: document.getElementById('formAnswer').value.trim(),
            keywords: document.getElementById('formKeywords').value.trim(),
            status: Number(document.getElementById('formStatus').value)
        };
        if (!payload.question) {
            App.toast('标准问题不能为空', 'error');
            return;
        }
        if (!payload.answer) {
            App.toast('标准答案不能为空', 'error');
            return;
        }
        try {
            await App.api('/api/admin/knowledge/save', { method: 'POST', body: payload });
            App.toast('保存成功', 'success');
            closeModal();
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function findById(id) {
        const page = await App.api('/api/admin/knowledge/page?current=1&size=100');
        return (page.records || []).find(function (k) {
            return String(k.id) === String(id);
        });
    }

    // ---------------- 事件 ----------------

    document.getElementById('searchBtn').addEventListener('click', function () {
        state.current = 1;
        state.keyword = document.getElementById('keyword').value.trim();
        state.categoryId = document.getElementById('categoryFilter').value;
        state.status = document.getElementById('statusFilter').value;
        load();
    });

    document.getElementById('keyword').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') document.getElementById('searchBtn').click();
    });

    document.getElementById('addBtn').addEventListener('click', function () {
        openModal(null);
    });
    document.getElementById('modalClose').addEventListener('click', closeModal);
    document.getElementById('cancelBtn').addEventListener('click', closeModal);
    document.getElementById('saveBtn').addEventListener('click', save);
    modal.addEventListener('click', function (e) {
        if (e.target === modal) closeModal();
    });

    body.addEventListener('click', async function (e) {
        const editBtn = e.target.closest('[data-edit]');
        if (editBtn) {
            try {
                const item = await findById(editBtn.dataset.edit);
                if (item) openModal(item);
                else App.toast('未找到该条目', 'error');
            } catch (err) {
                App.toast(err.message, 'error');
            }
            return;
        }

        const statusBtn = e.target.closest('[data-status]');
        if (statusBtn) {
            const next = Number(statusBtn.dataset.cur) === 1 ? 0 : 1;
            try {
                await App.api('/api/admin/knowledge/' + statusBtn.dataset.status + '/status?status=' + next,
                    { method: 'PUT' });
                App.toast('状态已更新', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
            return;
        }

        const delBtn = e.target.closest('[data-del]');
        if (delBtn) {
            if (!confirm('确定删除该知识条目吗？')) return;
            try {
                await App.api('/api/admin/knowledge/' + delBtn.dataset.del, { method: 'DELETE' });
                App.toast('删除成功', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
        }
    });

    (async function init() {
        const user = await AdminLayout.init('knowledge', '知识库管理');
        if (!user) return;
        try {
            await loadCategories();
        } catch (e) {
            App.toast('分类加载失败：' + e.message, 'error');
        }
        load();
    })();
})();
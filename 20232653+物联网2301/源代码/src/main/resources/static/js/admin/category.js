/* 后台分类管理：列表、新增/编辑、删除 */
(function () {
    const body = document.getElementById('tableBody');
    const modal = document.getElementById('modal');

    async function load() {
        try {
            const list = await App.api('/api/admin/category/list');
            body.innerHTML = list.length === 0
                ? '<tr><td colspan="7" style="text-align:center;color:#8a90a8;padding:32px;">还没有分类</td></tr>'
                : list.map(function (c) {
                    return '<tr>'
                        + '<td>' + c.id + '</td>'
                        + '<td>' + App.esc(c.name) + '</td>'
                        + '<td>' + c.sort + '</td>'
                        + '<td><span class="badge badge-on">' + c.knowledgeCount + ' 条</span></td>'
                        + '<td class="cell-ellipsis">' + App.esc(c.description || '-') + '</td>'
                        + '<td>' + App.formatTime(c.updateTime) + '</td>'
                        + '<td class="actions">'
                        + '<button class="btn btn-ghost btn-sm" data-edit="' + c.id + '">编辑</button> '
                        + '<button class="btn btn-ghost btn-sm" data-del="' + c.id + '">删除</button>'
                        + '</td></tr>';
                }).join('');
        } catch (e) {
            body.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#e11d48;padding:24px;">'
                + App.esc(e.message) + '</td></tr>';
        }
    }

    function openModal(item) {
        document.getElementById('modalTitle').textContent = item ? '编辑分类' : '新增分类';
        document.getElementById('formId').value = item ? item.id : '';
        document.getElementById('formName').value = item ? item.name : '';
        document.getElementById('formSort').value = item ? item.sort : 0;
        document.getElementById('formDesc').value = item ? (item.description || '') : '';
        modal.classList.add('open');
    }

    function closeModal() {
        modal.classList.remove('open');
    }

    async function findById(id) {
        const list = await App.api('/api/admin/category/list');
        return list.find(function (c) {
            return String(c.id) === String(id);
        });
    }

    async function save() {
        const payload = {
            id: document.getElementById('formId').value || null,
            name: document.getElementById('formName').value.trim(),
            sort: Number(document.getElementById('formSort').value || 0),
            description: document.getElementById('formDesc').value.trim()
        };
        if (!payload.name) {
            App.toast('分类名称不能为空', 'error');
            return;
        }
        try {
            await App.api('/api/admin/category/save', { method: 'POST', body: payload });
            App.toast('保存成功', 'success');
            closeModal();
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

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
            } catch (err) {
                App.toast(err.message, 'error');
            }
            return;
        }

        const delBtn = e.target.closest('[data-del]');
        if (delBtn) {
            if (!confirm('确定删除该分类吗？分类下存在知识条目时无法删除。')) return;
            try {
                await App.api('/api/admin/category/' + delBtn.dataset.del, { method: 'DELETE' });
                App.toast('删除成功', 'success');
                load();
            } catch (err) {
                App.toast(err.message, 'error');
            }
        }
    });

    (async function init() {
        const user = await AdminLayout.init('category', '分类管理');
        if (!user) return;
        load();
    })();
})();
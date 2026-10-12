/* 后台系统配置：读取与保存问答引擎参数、站点信息 */
(function () {
    const KEYS = {
        mode: 'ai.engine.mode',
        threshold: 'ai.engine.threshold',
        model: 'ai.llm.model',
        apiKey: 'ai.llm.apiKey',
        baseUrl: 'ai.llm.baseUrl',
        siteName: 'site.name',
        siteNotice: 'site.notice'
    };

    const body = document.getElementById('tableBody');

    async function load() {
        try {
            const list = await App.api('/api/admin/config/list');
            const map = {};
            list.forEach(function (c) {
                map[c.configKey] = c.configValue;
            });

            document.getElementById('mode').value = map[KEYS.mode] || 'local';
            document.getElementById('threshold').value = map[KEYS.threshold] || '0.25';
            document.getElementById('model').value = map[KEYS.model] || '';
            document.getElementById('apiKey').value = map[KEYS.apiKey] || '';
            document.getElementById('baseUrl').value = map[KEYS.baseUrl] || '';
            document.getElementById('siteName').value = map[KEYS.siteName] || '';
            document.getElementById('siteNotice').value = map[KEYS.siteNotice] || '';

            body.innerHTML = list.map(function (c) {
                return '<tr>'
                    + '<td>' + App.esc(c.configKey) + '</td>'
                    + '<td class="cell-ellipsis" title="' + App.esc(c.configValue) + '">'
                    + App.esc(c.configValue || '（空）') + '</td>'
                    + '<td>' + App.esc(c.remark || '-') + '</td>'
                    + '<td>' + App.formatTime(c.updateTime) + '</td>'
                    + '</tr>';
            }).join('');
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function saveKey(key, value) {
        return App.api('/api/admin/config/save', {
            method: 'PUT',
            body: { configKey: key, configValue: value }
        });
    }

    document.getElementById('saveBtn').addEventListener('click', async function () {
        const threshold = document.getElementById('threshold').value.trim();
        const value = Number(threshold);
        if (isNaN(value) || value < 0 || value > 1) {
            App.toast('命中阈值必须是 0~1 之间的数字', 'error');
            return;
        }
        try {
            await saveKey(KEYS.mode, document.getElementById('mode').value);
            await saveKey(KEYS.threshold, threshold);
            await saveKey(KEYS.model, document.getElementById('model').value.trim());
            await saveKey(KEYS.apiKey, document.getElementById('apiKey').value.trim());
            await saveKey(KEYS.baseUrl, document.getElementById('baseUrl').value.trim());
            App.toast('配置已保存并生效', 'success');
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    });

    document.getElementById('saveSiteBtn').addEventListener('click', async function () {
        try {
            await saveKey(KEYS.siteName, document.getElementById('siteName').value.trim());
            await saveKey(KEYS.siteNotice, document.getElementById('siteNotice').value.trim());
            App.toast('站点信息已保存', 'success');
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    });

    document.getElementById('reloadBtn').addEventListener('click', load);

    (async function init() {
        const user = await AdminLayout.init('config', '系统配置');
        if (!user) return;
        load();
    })();
})();
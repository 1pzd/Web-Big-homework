/* =====================================================================
   智问 · AI 智能问答平台  前端公共脚本
   职责：统一请求封装、登录态处理、轻提示、通用工具函数
   ===================================================================== */

const App = (function () {

    /**
     * 统一请求：自动带 Cookie、自动解析统一响应体、自动处理 401/403。
     * options.silent = true 时不弹出提示、不跳转登录页（用于探测登录态的接口）。
     */
    async function api(url, options) {
        const opts = Object.assign({ credentials: 'same-origin', headers: {} }, options || {});
        const silent = opts.silent === true;
        delete opts.silent;

        if (opts.body && typeof opts.body !== 'string') {
            opts.headers['Content-Type'] = 'application/json';
            opts.body = JSON.stringify(opts.body);
        }

        let res;
        try {
            res = await fetch(url, opts);
        } catch (e) {
            throw new Error('网络连接失败，请检查服务是否已启动');
        }

        if (res.status === 401) {
            if (!silent) {
                toast('登录已失效，请重新登录', 'error');
                setTimeout(function () {
                    location.href = location.pathname.indexOf('/admin') === 0 ? '/admin/login' : '/login';
                }, 900);
            }
            throw new Error('未登录');
        }
        if (res.status === 403) {
            if (!silent) {
                toast('没有访问权限', 'error');
            }
            throw new Error('无权限');
        }

        let data;
        try {
            data = await res.json();
        } catch (e) {
            throw new Error('服务返回格式异常');
        }
        if (data.code !== 200) {
            throw new Error(data.message || '操作失败');
        }
        return data.data;
    }

    /** 轻提示 */
    function toast(message, type) {
        let wrap = document.querySelector('.toast-wrap');
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.className = 'toast-wrap';
            document.body.appendChild(wrap);
        }
        const el = document.createElement('div');
        el.className = 'toast ' + (type || '');
        el.textContent = message;
        wrap.appendChild(el);
        setTimeout(function () {
            el.style.opacity = '0';
            el.style.transition = 'opacity .3s';
            setTimeout(function () {
                el.remove();
            }, 300);
        }, 2400);
    }

    /** 转义，防止用户输入内容破坏页面结构 */
    function esc(text) {
        if (text === null || text === undefined) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /** 时间格式化：yyyy-MM-dd HH:mm */
    function formatTime(value) {
        if (!value) return '';
        const d = new Date(String(value).replace(/-/g, '/'));
        if (isNaN(d.getTime())) return String(value);
        const p = function (n) {
            return n < 10 ? '0' + n : '' + n;
        };
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
            + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
    }

    /** 相对时间 */
    function fromNow(value) {
        if (!value) return '';
        const d = new Date(String(value).replace(/-/g, '/'));
        const diff = Date.now() - d.getTime();
        if (isNaN(diff)) return '';
        const min = Math.floor(diff / 60000);
        if (min < 1) return '刚刚';
        if (min < 60) return min + ' 分钟前';
        const hour = Math.floor(min / 60);
        if (hour < 24) return hour + ' 小时前';
        const day = Math.floor(hour / 24);
        if (day < 30) return day + ' 天前';
        return formatTime(value);
    }

    /** 获取当前用户，未登录返回 null（静默探测，不触发跳转） */
    async function currentUser() {
        try {
            return await api('/api/auth/me', { silent: true });
        } catch (e) {
            return null;
        }
    }

    /** 退出登录 */
    async function logout() {
        try {
            await api('/api/auth/logout', { method: 'POST' });
        } catch (e) {
            /* 忽略：无论服务端结果如何都跳转登录页 */
        }
        location.href = '/login';
    }

    /** 绑定退出按钮 */
    function bindLogout(selector) {
        const el = document.querySelector(selector || '[data-logout]');
        if (el) {
            el.addEventListener('click', function (e) {
                e.preventDefault();
                logout();
            });
        }
    }

    /** 取查询参数 */
    function query(name) {
        return new URLSearchParams(location.search).get(name);
    }

    /** 渲染问答来源标签 */
    function sourceTag(source) {
        const map = {
            knowledge: ['tag-knowledge', '知识库命中'],
            llm: ['tag-llm', '大模型生成'],
            fallback: ['tag-fallback', '未命中']
        };
        const item = map[source] || map.fallback;
        return '<span class="tag ' + item[0] + '">' + item[1] + '</span>';
    }

    return {
        api: api,
        toast: toast,
        esc: esc,
        formatTime: formatTime,
        fromNow: fromNow,
        currentUser: currentUser,
        logout: logout,
        bindLogout: bindLogout,
        query: query,
        sourceTag: sourceTag
    };
})();
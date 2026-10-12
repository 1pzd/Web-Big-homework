/* 智能问答页：会话管理 + 消息收发 + 答案反馈 */
(function () {
    const state = {
        sessionId: null,
        sending: false
    };

    const body = document.getElementById('chatBody');
    const input = document.getElementById('askInput');
    const sendBtn = document.getElementById('sendBtn');
    const sessionList = document.getElementById('sessionList');
    const guessRow = document.getElementById('guessRow');
    const chatTitle = document.getElementById('chatTitle');

    // ---------------- 渲染 ----------------

    function welcomeHtml() {
        return '<div class="empty" id="welcomeBox">'
            + '<div class="empty-icon">◇</div>'
            + '<div style="font-size:17px;font-weight:600;color:#1b1f36;margin-bottom:6px;">你好，我是智问助手</div>'
            + '<div>试着问我一个问题吧，例如「如何注册账号？」或「校园网怎么连接？」</div>'
            + '</div>';
    }

    function messageHtml(msg) {
        const isUser = msg.role === 'user';
        const avatarText = isUser ? '我' : '智';
        let meta = '';
        if (!isUser) {
            const score = (msg.score === null || msg.score === undefined)
                ? '' : '相似度 ' + msg.score;
            const cost = (msg.costMs === null || msg.costMs === undefined)
                ? '' : msg.costMs + ' ms';
            meta = '<div class="msg-meta">'
                + App.sourceTag(msg.source)
                + (score ? '<span>' + App.esc(score) + '</span>' : '')
                + (cost ? '<span>耗时 ' + App.esc(cost) + '</span>' : '')
                + (msg.matchedQuestion ? '<span>依据：' + App.esc(msg.matchedQuestion) + '</span>' : '');
            if (msg.id) {
                meta += '<button class="fb-btn" data-fb="1" data-id="' + msg.id + '">有帮助</button>'
                    + '<button class="fb-btn" data-fb="-1" data-id="' + msg.id + '">没帮助</button>';
            }
            meta += '</div>';
        }
        return '<div class="msg ' + (isUser ? 'user' : 'assistant') + '">'
            + '<div class="msg-avatar">' + avatarText + '</div>'
            + '<div><div class="bubble">' + App.esc(msg.content) + '</div>' + meta + '</div>'
            + '</div>';
    }

    function appendMessage(msg) {
        const welcome = document.getElementById('welcomeBox');
        if (welcome) welcome.remove();
        body.insertAdjacentHTML('beforeend', messageHtml(msg));
        body.scrollTop = body.scrollHeight;
    }

    function showTyping() {
        body.insertAdjacentHTML('beforeend',
            '<div class="msg assistant" id="typingBox">'
            + '<div class="msg-avatar">智</div>'
            + '<div class="bubble"><span class="typing"><i></i><i></i><i></i></span></div>'
            + '</div>');
        body.scrollTop = body.scrollHeight;
    }

    function hideTyping() {
        const box = document.getElementById('typingBox');
        if (box) box.remove();
    }

    function renderSessions(list) {
        if (!list || list.length === 0) {
            sessionList.innerHTML = '<div style="padding:16px;color:#8a90a8;font-size:13px;text-align:center;">'
                + '还没有历史会话</div>';
            return;
        }
        sessionList.innerHTML = list.map(function (s) {
            const active = String(s.id) === String(state.sessionId) ? ' active' : '';
            return '<div class="session-item' + active + '" data-id="' + s.id + '" data-title="'
                + App.esc(s.title) + '">'
                + '<div class="t">' + App.esc(s.title || '未命名会话') + '</div>'
                + '<div class="m">' + s.messageCount + ' 条消息 · ' + App.fromNow(s.lastTime) + '</div>'
                + '</div>';
        }).join('');
    }

    // ---------------- 数据加载 ----------------

    async function loadSessions() {
        try {
            const page = await App.api('/api/chat/sessions?current=1&size=50');
            renderSessions(page.records);
        } catch (e) {
            sessionList.innerHTML = '<div style="padding:16px;color:#e11d48;font-size:13px;">加载失败</div>';
        }
    }

    async function openSession(id, title) {
        state.sessionId = id;
        chatTitle.textContent = title || '对话';
        body.innerHTML = '';
        try {
            const messages = await App.api('/api/chat/messages/' + id);
            if (!messages || messages.length === 0) {
                body.innerHTML = welcomeHtml();
            } else {
                messages.forEach(appendMessage);
            }
        } catch (e) {
            App.toast(e.message, 'error');
        }
        renderSessions((await App.api('/api/chat/sessions?current=1&size=50')).records);
    }

    function newSession() {
        state.sessionId = null;
        chatTitle.textContent = '新的对话';
        body.innerHTML = welcomeHtml();
        loadSessions();
        input.focus();
    }

    async function loadGuess() {
        try {
            const list = await App.api('/api/chat/guess?limit=4');
            if (!list || list.length === 0) return;
            guessRow.innerHTML = list.map(function (q) {
                return '<span class="chip" data-q="' + App.esc(q) + '">' + App.esc(q) + '</span>';
            }).join('');
        } catch (e) {
            /* 推荐问题加载失败不影响提问 */
        }
    }

    // ---------------- 提问 ----------------

    async function send() {
        const question = input.value.trim();
        if (!question || state.sending) return;

        state.sending = true;
        sendBtn.disabled = true;
        input.value = '';
        input.style.height = 'auto';

        appendMessage({ role: 'user', content: question });
        showTyping();

        try {
            const res = await App.api('/api/chat/ask', {
                method: 'POST',
                body: { sessionId: state.sessionId, question: question }
            });
            hideTyping();
            state.sessionId = res.sessionId;
            chatTitle.textContent = res.sessionTitle || '对话';
            appendMessage({
                role: 'assistant',
                content: res.answer,
                source: res.source,
                score: res.score,
                costMs: res.costMs,
                matchedQuestion: res.matchedQuestion
            });
            // 重新拉取会话列表以拿到消息中携带的消息 ID（用于反馈按钮）
            const page = await App.api('/api/chat/sessions?current=1&size=50');
            renderSessions(page.records);
            if (res.sessionId) {
                const messages = await App.api('/api/chat/messages/' + res.sessionId);
                body.innerHTML = '';
                messages.forEach(appendMessage);
            }
        } catch (e) {
            hideTyping();
            appendMessage({ role: 'assistant', content: '抱歉，' + e.message, source: 'fallback' });
        } finally {
            state.sending = false;
            sendBtn.disabled = false;
            input.focus();
        }
    }

    // ---------------- 事件绑定 ----------------

    sendBtn.addEventListener('click', send);

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            send();
        }
    });

    input.addEventListener('input', function () {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 150) + 'px';
    });

    document.getElementById('newSessionBtn').addEventListener('click', newSession);

    sessionList.addEventListener('click', function (e) {
        const item = e.target.closest('.session-item');
        if (!item) return;
        openSession(item.dataset.id, item.dataset.title);
    });

    guessRow.addEventListener('click', function (e) {
        const chip = e.target.closest('.chip');
        if (!chip) return;
        input.value = chip.dataset.q;
        send();
    });

    body.addEventListener('click', async function (e) {
        const btn = e.target.closest('.fb-btn');
        if (!btn) return;
        const value = Number(btn.dataset.fb);
        const messageId = Number(btn.dataset.id);
        try {
            await App.api('/api/chat/feedback', {
                method: 'POST',
                body: { messageId: messageId, feedback: value }
            });
            const siblings = btn.parentElement.querySelectorAll('.fb-btn');
            siblings.forEach(function (b) {
                b.classList.remove('on');
            });
            btn.classList.add('on');
            App.toast('感谢你的反馈', 'success');
        } catch (err) {
            App.toast(err.message, 'error');
        }
    });

    // ---------------- 初始化 ----------------

    (async function init() {
        const user = await App.currentUser();
        if (user) {
            const name = user.nickname || user.username;
            document.getElementById('userName').textContent = name;
            document.getElementById('userAvatar').textContent = name.substring(0, 1);
        }
        App.bindLogout('[data-logout]');

        await loadSessions();
        await loadGuess();
        body.innerHTML = welcomeHtml();

        const preset = App.query('q');
        if (preset) {
            input.value = preset;
            send();
        } else {
            input.focus();
        }
    })();
})();
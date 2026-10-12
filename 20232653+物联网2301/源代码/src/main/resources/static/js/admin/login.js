/* 后台登录页 */
(function () {
    const form = document.getElementById('adminLoginForm');
    const errBox = document.getElementById('errBox');
    const submitBtn = document.getElementById('submitBtn');

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        errBox.classList.remove('show');

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        submitBtn.disabled = true;
        submitBtn.textContent = '登录中…';
        try {
            await App.api('/api/auth/admin/login', {
                method: 'POST',
                body: { username: username, password: password }
            });
            App.toast('登录成功', 'success');
            setTimeout(function () {
                location.href = '/admin';
            }, 500);
        } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.add('show');
            submitBtn.disabled = false;
            submitBtn.textContent = '登 录 后 台';
        }
    });
})();
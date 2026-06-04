(function () {
    const apiBase = window.CLOUD_STUDY_API_BASE || '/api';
    const configKeys = [
        'db.endpoint',
        'db.username',
        'db.password',
        'storage.account',
        'storage.container',
        'storage.sas',
        'storage.file.share',
        'storage.file.directory',
        'was.baseUrl'
    ];
    const sensitiveKeys = ['db.password', 'storage.sas'];

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function fetchJson(path, options) {
        const response = await fetch(apiBase + path, options);
        const text = await response.text();
        let body = {};

        try {
            body = text ? JSON.parse(text) : {};
        } catch (e) {
            body = { error: text || 'JSON 응답을 해석할 수 없습니다.' };
        }

        if (!response.ok || body.error) {
            throw new Error(body.error || body.message || `HTTP ${response.status}`);
        }

        return body;
    }

    function showMessage(message, type, targetId) {
        const element = document.getElementById(targetId || 'message');
        if (!element) {
            return;
        }
        element.className = `alert ${type || 'success'}`;
        element.textContent = message;
    }

    function findInput(key) {
        return document.querySelector(`input[name="${key}"]`);
    }

    function formBody(form) {
        const formData = new FormData(form);
        return new URLSearchParams(formData);
    }

    function renderConfig(data) {
        const values = data.values || {};
        const table = document.getElementById('configTable');
        const path = document.getElementById('configPath');

        if (path) {
            path.textContent = data.configFilePath || '경로를 조회할 수 없습니다.';
        }

        if (table) {
            table.innerHTML = configKeys.map((key) => {
                const className = sensitiveKeys.includes(key) ? ' class="highlight"' : '';
                return `<tr><th>${escapeHtml(key)}</th><td${className}>${escapeHtml(values[key] || '')}</td></tr>`;
            }).join('');
        }

        configKeys.forEach((key) => {
            if (sensitiveKeys.includes(key)) {
                return;
            }
            const input = findInput(key);
            if (input) {
                input.value = values[key] || '';
            }
        });
    }

    async function loadConfig() {
        try {
            renderConfig(await fetchJson('/config'));
        } catch (e) {
            showMessage(e.message, 'error');
        }
    }

    async function saveConfig(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const body = new URLSearchParams();

        configKeys.forEach((key) => {
            body.append(key, formData.get(key) || '');
        });

        try {
            const data = await fetchJson('/config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body
            });
            renderConfig(data);
            showMessage(data.message || '설정 파일을 저장했습니다.', 'success');

            sensitiveKeys.forEach((key) => {
                const input = findInput(key);
                if (input) {
                    input.value = '';
                }
            });
        } catch (e) {
            showMessage(e.message, 'error');
        }
    }

    function renderStatus(data) {
        const labels = {
            hostName: '호스트명',
            privateIp: 'Private IPv4',
            instanceId: '인스턴스 식별값',
            usedMemoryBytes: 'JVM 사용 메모리(bytes)',
            maxMemoryBytes: 'JVM 최대 메모리(bytes)',
            systemCpuLoad: '시스템 CPU 사용률',
            processCpuLoad: 'JVM 프로세스 CPU 사용률',
            currentTime: '현재 시간',
            clientIp: '요청 클라이언트 IP',
            xForwardedFor: 'X-Forwarded-For',
            xForwardedProto: 'X-Forwarded-Proto',
            xForwardedHost: 'X-Forwarded-Host',
            host: 'Host',
            userAgent: 'User-Agent',
            proxyOrLoadBalancerGuess: '프록시/로드밸런서 경유 추정'
        };
        const table = document.getElementById('statusTable');
        table.innerHTML = Object.keys(labels).map((key) => {
            const className = key === 'instanceId' || key === 'privateIp' ? ' class="highlight"' : '';
            return `<tr><th>${labels[key]}</th><td${className}>${escapeHtml(data[key] || '')}</td></tr>`;
        }).join('');
    }

    async function loadStatus() {
        const table = document.getElementById('statusTable');
        table.innerHTML = '<tr><td>조회 중입니다...</td></tr>';
        try {
            renderStatus(await fetchJson('/server-status'));
        } catch (e) {
            table.innerHTML = `<tr><td class="warning">${escapeHtml(e.message)}</td></tr>`;
        }
    }

    async function callHealth(targetId) {
        const target = document.getElementById(targetId);
        target.textContent = '호출 중입니다...';
        try {
            target.textContent = JSON.stringify(await fetchJson('/health'), null, 2);
        } catch (e) {
            target.textContent = e.message;
        }
    }

    async function loadDbItems() {
        const table = document.getElementById('dbTable');
        table.innerHTML = '<tr><td>DB 조회 중입니다...</td></tr>';
        try {
            const data = await fetchJson('/db/items');
            const items = data.items || [];
            if (items.length === 0) {
                table.innerHTML = '<tr><td>저장된 항목이 없습니다.</td></tr>';
                return;
            }
            table.innerHTML = items.map((item) => `
                <tr>
                    <td>${escapeHtml(item.id)}</td>
                    <td>${escapeHtml(item.assetName)}</td>
                    <td>${escapeHtml(item.assetType)}</td>
                    <td>${escapeHtml(item.ownerName)}</td>
                    <td>${escapeHtml(item.status)}</td>
                    <td>${escapeHtml(item.description)}</td>
                    <td>${escapeHtml(item.updatedAt)}</td>
                    <td>
                        <button type="button"
                                data-edit-id="${escapeHtml(item.id)}"
                                data-asset-name="${escapeHtml(item.assetName)}"
                                data-asset-type="${escapeHtml(item.assetType)}"
                                data-owner-name="${escapeHtml(item.ownerName)}"
                                data-status="${escapeHtml(item.status)}"
                                data-description="${escapeHtml(item.description)}">수정 폼</button>
                        <button type="button" data-delete-id="${escapeHtml(item.id)}">삭제</button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            table.innerHTML = `<tr><td class="warning">${escapeHtml(e.message)}</td></tr>`;
        }
    }

    async function submitDbForm(event) {
        event.preventDefault();
        const id = document.getElementById('dbId').value;
        const path = id ? '/db/items/update' : '/db/items';
        try {
            const data = await fetchJson(path, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: formBody(event.target)
            });
            showMessage(data.message || 'DB 작업을 완료했습니다.', 'success', 'dbMessage');
            event.target.reset();
            await loadDbItems();
        } catch (e) {
            showMessage(e.message, 'error', 'dbMessage');
        }
    }

    async function testDbConnection() {
        const target = document.getElementById('dbTestResult');
        target.textContent = '연결 확인 중입니다...';
        try {
            target.textContent = JSON.stringify(await fetchJson('/db/test'), null, 2);
        } catch (e) {
            target.textContent = e.message;
        }
    }

    async function deleteDbItem(id) {
        const body = new URLSearchParams();
        body.append('id', id);
        try {
            const data = await fetchJson('/db/items/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body
            });
            showMessage(data.message || '삭제했습니다.', 'success', 'dbMessage');
            await loadDbItems();
        } catch (e) {
            showMessage(e.message, 'error', 'dbMessage');
        }
    }

    function fillDbForm(button) {
        document.getElementById('dbId').value = button.dataset.editId;
        document.getElementById('dbAssetName').value = button.dataset.assetName || '';
        document.getElementById('dbAssetType').value = button.dataset.assetType || 'ETC';
        document.getElementById('dbOwnerName').value = button.dataset.ownerName || '';
        document.getElementById('dbStatus').value = button.dataset.status || 'READY';
        document.getElementById('dbDescription').value = button.dataset.description || '';
    }

    async function loadStorageList(type) {
        const table = document.getElementById(type + 'Table');
        table.innerHTML = '<tr><td>목록 조회 중입니다...</td></tr>';
        try {
            const data = await fetchJson(`/storage/${type}/list`);
            const items = data.items || [];
            if (items.length === 0) {
                table.innerHTML = '<tr><td>표시할 파일이 없습니다.</td></tr>';
                return;
            }
            table.innerHTML = items.map((item) => `
                <tr>
                    <td>${escapeHtml(item.name)}</td>
                    <td>${escapeHtml(item.type)}</td>
                    <td><button type="button" data-storage-type="${type}" data-storage-delete="${escapeHtml(item.name)}">삭제</button></td>
                </tr>
            `).join('');
        } catch (e) {
            table.innerHTML = `<tr><td class="warning">${escapeHtml(e.message)}</td></tr>`;
        }
    }

    async function uploadStorage(event, type) {
        event.preventDefault();
        try {
            const data = await fetchJson(`/storage/${type}/upload`, {
                method: 'POST',
                body: new FormData(event.target)
            });
            showMessage(data.message || '업로드했습니다.', 'success', type + 'Message');
            event.target.reset();
            await loadStorageList(type);
        } catch (e) {
            showMessage(e.message, 'error', type + 'Message');
        }
    }

    async function deleteStorage(type, name) {
        const body = new URLSearchParams();
        body.append('name', name);
        try {
            const data = await fetchJson(`/storage/${type}/delete`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body
            });
            showMessage(data.message || '삭제했습니다.', 'success', type + 'Message');
            await loadStorageList(type);
        } catch (e) {
            showMessage(e.message, 'error', type + 'Message');
        }
    }

    window.CloudStudy = {
        callHealth,
        initConfigPage: function () {
            document.getElementById('configForm').addEventListener('submit', saveConfig);
            loadConfig();
        },
        initStatusPage: function () {
            document.getElementById('refreshButton').addEventListener('click', loadStatus);
            loadStatus();
        },
        initDbPage: function () {
            document.getElementById('dbForm').addEventListener('submit', submitDbForm);
            document.getElementById('dbTestButton').addEventListener('click', testDbConnection);
            document.getElementById('dbReloadButton').addEventListener('click', loadDbItems);
            document.getElementById('dbCancelButton').addEventListener('click', function () {
                document.getElementById('dbForm').reset();
            });
            document.getElementById('dbTable').addEventListener('click', function (event) {
                if (event.target.dataset.editId) {
                    fillDbForm(event.target);
                }
                if (event.target.dataset.deleteId) {
                    deleteDbItem(event.target.dataset.deleteId);
                }
            });
            loadDbItems();
        },
        initStoragePage: function () {
            document.getElementById('blobForm').addEventListener('submit', function (event) {
                uploadStorage(event, 'blob');
            });
            document.getElementById('fileForm').addEventListener('submit', function (event) {
                uploadStorage(event, 'file');
            });
            document.getElementById('blobReloadButton').addEventListener('click', function () {
                loadStorageList('blob');
            });
            document.getElementById('fileReloadButton').addEventListener('click', function () {
                loadStorageList('file');
            });
            document.addEventListener('click', function (event) {
                if (event.target.dataset.storageDelete) {
                    deleteStorage(event.target.dataset.storageType, event.target.dataset.storageDelete);
                }
            });
            loadStorageList('blob');
            loadStorageList('file');
        }
    };
})();

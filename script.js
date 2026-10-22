const notice = document.getElementById('notice');

function showNotice(message, isError = false) {
  notice.textContent = message;
  notice.classList.toggle('error', isError);
}

async function api(path, method = 'GET', data = undefined) {
  const options = { method, headers: {} };
  if (data !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(data);
  }
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'The request could not be completed.');
  return result;
}

function renderList(elementId, countId, items, emptyText) {
  const list = document.getElementById(elementId);
  const count = document.getElementById(countId);
  count.textContent = `${items.length} ${elementId === 'book-list' ? (items.length === 1 ? 'book' : 'books') : (items.length === 1 ? 'student' : 'students')}`;
  list.replaceChildren();
  if (!items.length) {
    const li = document.createElement('li');
    li.className = 'empty';
    li.textContent = emptyText;
    list.appendChild(li);
    return;
  }
  items.forEach((item, index) => {
    const li = document.createElement('li');
    const number = document.createElement('span');
    number.className = 'number';
    number.textContent = String(index + 1).padStart(2, '0');
    const label = document.createElement('span');
    label.textContent = item;
    li.append(number, label);
    list.appendChild(li);
  });
}

async function refreshBooks() {
  const data = await api('/api/books');
  renderList('book-list', 'book-count', data.items, 'No returned books yet.');
}
async function refreshStudents() {
  const data = await api('/api/waitlist');
  renderList('student-list', 'student-count', data.items, 'No students waiting.');
}
async function refreshAll() {
  try {
    await Promise.all([refreshBooks(), refreshStudents()]);
  } catch (error) {
    showNotice('Could not connect to Java. Start the server using run.bat or run.sh.', true);
  }
}

document.getElementById('book-form').addEventListener('submit', async event => {
  event.preventDefault();
  const input = document.getElementById('book-input');
  const book = input.value.trim();
  if (!book) return;
  try {
    const result = await api('/api/books', 'POST', { book });
    input.value = '';
    await refreshBooks();
    showNotice(result.message);
    input.focus();
  } catch (error) { showNotice(error.message, true); }
});

document.getElementById('pop-book').addEventListener('click', async () => {
  try {
    const result = await api('/api/books/pop', 'POST');
    await refreshBooks();
    showNotice(result.item ? `Processed: ${result.item}` : result.message);
  } catch (error) { showNotice(error.message, true); }
});

document.getElementById('student-form').addEventListener('submit', async event => {
  event.preventDefault();
  const input = document.getElementById('student-input');
  const student = input.value.trim();
  if (!student) return;
  try {
    const result = await api('/api/waitlist', 'POST', { student });
    input.value = '';
    await refreshStudents();
    showNotice(result.message);
    input.focus();
  } catch (error) { showNotice(error.message, true); }
});

document.getElementById('serve-student').addEventListener('click', async () => {
  try {
    const result = await api('/api/waitlist/dequeue', 'POST');
    await refreshStudents();
    showNotice(result.item ? `Now serving: ${result.item}` : result.message);
  } catch (error) { showNotice(error.message, true); }
});

document.getElementById('refresh-books').addEventListener('click', async () => {
  try { await refreshBooks(); showNotice('Returned-books stack refreshed.'); }
  catch (error) { showNotice(error.message, true); }
});
document.getElementById('refresh-students').addEventListener('click', async () => {
  try { await refreshStudents(); showNotice('Student queue refreshed.'); }
  catch (error) { showNotice(error.message, true); }
});

refreshAll();

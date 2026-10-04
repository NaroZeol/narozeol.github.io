import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { flushSync } from 'react-dom';
import DatePicker from '@arco-design/web-react/es/DatePicker';
import ConfigProvider from '@arco-design/web-react/es/ConfigProvider';
import zhCN from '@arco-design/web-react/es/locale/zh-CN';
import dayjs from 'dayjs';
import '@arco-design/web-react/es/DatePicker/style/css.js';
import { publicationDay } from '../../assets/js/thought-date-picker.mjs';

const today = () => dayjs(publicationDay(new Date().toISOString()));
const shortcuts = [
  { text: '最近 7 天', value: () => [today().subtract(6, 'day'), today()] },
  { text: '最近 30 天', value: () => [today().subtract(29, 'day'), today()] },
  { text: '本月', value: () => [today().startOf('month'), today()] },
  { text: '今年', value: () => [today().startOf('year'), today()] },
];

export function createDatePicker(onChange) {
  const host = document.querySelector('#thought-date-root');
  let applied = { start: '', end: '' };
  let reset;

  function Picker() {
    const [value, setValue] = useState([]);
    const [visible, setVisible] = useState(false);
    const [error, setError] = useState('');
    reset = () => { setValue([]); setVisible(false); setError(''); };

    function commit(values) {
      const next = values || [];
      applied = { start: next[0] || '', end: next[1] || '' };
      setValue(next);
      setError('');
      onChange();
    }

    function validateInput(event) {
      const text = event.target.value;
      const valid = !text || (/^\d{4}-\d{2}-\d{2}$/.test(text) && dayjs(text).format('YYYY-MM-DD') === text);
      setError(valid ? '' : '请输入有效日期，格式为 YYYY-MM-DD');
      return valid;
    }

    return <ConfigProvider locale={zhCN}>
      <div onKeyDownCapture={event => {
        if (event.key === 'Escape') {
          event.stopPropagation();
          setVisible(false);
          setError('');
        }
        if (event.key === 'Enter' && event.target.tagName === 'INPUT' && !validateInput(event)) {
          event.preventDefault();
          event.stopPropagation();
        }
      }}>
        <DatePicker.RangePicker
          value={value}
          onChange={commit}
          popupVisible={visible}
          onVisibleChange={next => { setVisible(next); if (!next) setError(''); }}
          format="YYYY-MM-DD"
          placeholder={['开始日期', '结束日期']}
          separator="至"
          editable
          allowClear
          dayStartOfWeek={1}
          defaultPickerValue={[today(), today().add(1, 'month')]}
          shortcuts={shortcuts}
          getPopupContainer={() => host}
          triggerProps={{ className: 'thought-date-popup', autoFitPosition: true, autoFitTransformOrigin: true }}
          inputProps={['start', 'end'].map((endpoint, index) => ({
            id: `thought-date-${endpoint}`,
            'aria-label': index === 0 ? '开始日期' : '结束日期',
            'aria-describedby': error ? 'thought-date-error' : undefined,
            'aria-invalid': Boolean(error),
            autoComplete: 'off',
            onChange: () => setError(''),
            onBlur: validateInput,
          }))}
          status={error ? 'error' : undefined}
          unmountOnExit
        />
        {error && <p id="thought-date-error" className="thought-date-error" role="alert">{error}</p>}
      </div>
    </ConfigProvider>;
  }

  const root = createRoot(host);
  flushSync(() => root.render(<Picker />));
  return {
    get range() { return applied; },
    reset() {
      applied = { start: '', end: '' };
      flushSync(reset);
    },
  };
}

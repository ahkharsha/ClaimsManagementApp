import { useState, useEffect } from 'react';
import { Bell, CheckCheck, Trash2, Filter } from 'lucide-react';
import { socketService } from '../../services/socketService';
import { springApi } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const typeConfig = {
  CLAIM_FILED: { icon: '📋', label: 'Claim Filed', color: 'border-l-blue-500' },
  CLAIM_STATUS_UPDATED: { icon: '🔄', label: 'Status Updated', color: 'border-l-emerald-500' },
  FRAUD_ALERT: { icon: '🚨', label: 'Fraud Alert', color: 'border-l-red-500' },
  POLICY_RENEWAL_DUE: { icon: '🔔', label: 'Renewal Due', color: 'border-l-amber-500' },
  PREMIUM_PAYMENT_RECEIVED: { icon: '💰', label: 'Payment', color: 'border-l-green-500' },
  CLAIM_SETTLED: { icon: '✅', label: 'Claim Settled', color: 'border-l-teal-500' },
  TEST: { icon: '🧪', label: 'Test', color: 'border-l-purple-500' },
  SYSTEM: { icon: '⚙️', label: 'System', color: 'border-l-slate-500' },
};

export default function NotificationsCenter() {
  const [notifications, setNotifications] = useState([]);
  const [filter, setFilter] = useState('all');
  const [socketConnected, setSocketConnected] = useState(false);
  const { user } = useAuth();

  useEffect(() => {
    // Fetch existing notifications from REST API
    const fetchNotifications = async () => {
      if (!user) return;
      try {
        const userId = user.id || user.userId;
        const res = await springApi.get(`/notifications/${userId}`);
        const fetched = res.data?.data?.notifications || [];
        setNotifications(fetched.map(n => ({
          id: n.notificationId,
          type: n.eventType,
          message: n.message,
          title: n.title,
          time: n.timestamp ? new Date(n.timestamp).toLocaleString() : 'Unknown',
          read: n.isRead,
        })));
      } catch (err) {
        console.log('Could not fetch notifications from API:', err.message);
      }
    };

    fetchNotifications();

    // Connect Socket.IO for real-time notifications
    try {
      const sock = socketService.connect();

      if (sock) {
        sock.on('connect', () => setSocketConnected(true));
        sock.on('disconnect', () => setSocketConnected(false));

        // Listen for the generic 'notification' event emitted by the Node server
        socketService.on('notification', (data) => {
          setNotifications(prev => [{
            id: data.notificationId || Date.now(),
            type: data.eventType || 'SYSTEM',
            message: data.message,
            title: data.title,
            time: 'Just now',
            read: false,
          }, ...prev]);
        });
      }
    } catch {
      // Socket unavailable
    }

    return () => socketService.disconnect();
  }, [user]);

  const filteredNotifs = filter === 'all'
    ? notifications
    : filter === 'unread'
    ? notifications.filter(n => !n.read)
    : notifications.filter(n => n.type === filter);

  const unreadCount = notifications.filter(n => !n.read).length;

  const markAllRead = async () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    if (user) {
      try {
        const userId = user.id || user.userId;
        await springApi.put(`/notifications/read-all/${userId}`);
      } catch { /* silent */ }
    }
  };

  const markRead = async (id) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
    try {
      await springApi.put(`/notifications/read/${id}`);
    } catch { /* silent */ }
  };

  const deleteNotif = async (id) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
    try {
      await springApi.delete(`/notifications/${id}`);
    } catch { /* silent */ }
  };

  const filters = [
    { key: 'all', label: 'All' },
    { key: 'unread', label: `Unread (${unreadCount})` },
    { key: 'CLAIM_FILED', label: 'Claims' },
    { key: 'FRAUD_ALERT', label: 'Fraud' },
    { key: 'POLICY_RENEWAL_DUE', label: 'Renewals' },
    { key: 'CLAIM_STATUS_UPDATED', label: 'Status' },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Notifications Center</h1>
          <p className="text-sm text-slate-400 mt-1">
            {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}` : 'All caught up!'}
          </p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500/20 text-blue-400 text-sm font-medium rounded-xl hover:bg-blue-500/30 transition-colors"
          >
            <CheckCheck size={16} /> Mark All Read
          </button>
        )}
      </div>

      {/* Socket status indicator */}
      <div className="glass-card p-3 flex items-center gap-3">
        <div className={`w-2 h-2 rounded-full ${socketConnected ? 'bg-emerald-500' : 'bg-amber-500'} animate-pulse`} />
        <span className="text-xs text-slate-400">
          Real-time notifications via Socket.IO — {' '}
          <span className={socketConnected ? 'text-emerald-400' : 'text-amber-400'}>
            {socketConnected ? 'Connected' : 'Disconnected'}
          </span>
        </span>
      </div>

      {/* Filter tabs */}
      <div className="flex items-center gap-2 flex-wrap">
        <Filter size={16} className="text-slate-500" />
        {filters.map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all ${
              filter === f.key
                ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                : 'bg-slate-800/60 text-slate-400 border border-slate-700/50 hover:text-slate-200'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Notification list */}
      <div className="space-y-2">
        {filteredNotifs.length === 0 ? (
          <div className="glass-card p-12 text-center">
            <Bell size={40} className="text-slate-600 mx-auto mb-3" />
            <p className="text-slate-400">No notifications to show</p>
          </div>
        ) : (
          filteredNotifs.map((notif) => {
            const config = typeConfig[notif.type] || { icon: '📌', label: 'Notification', color: 'border-l-slate-500' };
            return (
              <div
                key={notif.id}
                onClick={() => markRead(notif.id)}
                className={`glass-card p-4 border-l-4 ${config.color} cursor-pointer transition-all hover:border-opacity-100 ${
                  !notif.read ? 'bg-blue-500/5' : ''
                }`}
              >
                <div className="flex items-start gap-3">
                  <span className="text-xl flex-shrink-0 mt-0.5">{config.icon}</span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{config.label}</span>
                      {!notif.read && <div className="w-2 h-2 rounded-full bg-blue-500" />}
                    </div>
                    {notif.title && (
                      <p className="text-sm font-semibold text-slate-200 mb-0.5">{notif.title}</p>
                    )}
                    <p className={`text-sm ${!notif.read ? 'text-slate-200 font-medium' : 'text-slate-400'}`}>
                      {notif.message}
                    </p>
                    <p className="text-[11px] text-slate-600 mt-1">{notif.time}</p>
                  </div>
                  <button
                    onClick={(e) => { e.stopPropagation(); deleteNotif(notif.id); }}
                    className="p-1.5 rounded-lg text-slate-600 hover:text-red-400 hover:bg-red-500/10 transition-colors flex-shrink-0"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

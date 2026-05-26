import { io } from 'socket.io-client';

const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || 'http://localhost:5001';

let socket = null;

export const socketService = {
  connect() {
    try {
      const token = localStorage.getItem('token');

      if (!token) {
        console.log('Socket: No auth token found — skipping connection');
        return null;
      }

      socket = io(SOCKET_URL, {
        auth: { token },
        transports: ['websocket', 'polling'],
        reconnection: true,
        reconnectionDelay: 1000,
        reconnectionAttempts: 5,
      });

      socket.on('connect', () => {
        console.log('Socket connected:', socket.id);
      });

      socket.on('disconnect', (reason) => {
        console.log('Socket disconnected:', reason);
      });

      socket.on('connect_error', (err) => {
        console.log('Socket connection error:', err.message);
      });

      return socket;
    } catch (err) {
      console.log('Socket unavailable:', err.message);
      return null;
    }
  },

  disconnect() {
    if (socket) {
      socket.disconnect();
      socket = null;
    }
  },

  on(event, callback) {
    if (socket) {
      socket.on(event, callback);
    }
  },

  off(event, callback) {
    if (socket) {
      socket.off(event, callback);
    }
  },

  emit(event, data) {
    if (socket) {
      socket.emit(event, data);
    }
  },

  getSocket() {
    return socket;
  }
};

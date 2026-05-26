// ============================================
// InsuranceIQ Notification Service — Socket.IO Setup
// ============================================

const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const { pool } = require('../config/db');

let io = null;

/**
 * Look up a user's numeric ID by their email address.
 * Spring Boot JWT uses email as the subject, but our notification
 * system routes by numeric user_id. This bridges the gap.
 */
async function resolveUserId(email) {
    try {
        const [rows] = await pool.execute(
            'SELECT id FROM users WHERE email = ? LIMIT 1',
            [email]
        );
        if (rows.length > 0) {
            return rows[0].id;
        }
    } catch (err) {
        console.log('[SOCKET] Could not resolve user ID from email:', err.message);
    }
    return null;
}

/**
 * Initialize Socket.IO server with JWT authentication
 * @param {http.Server} httpServer - The HTTP server instance
 * @returns {Server} Socket.IO server instance
 */
function initializeSocket(httpServer) {
    io = new Server(httpServer, {
        cors: {
            origin: '*',
            methods: ['GET', 'POST'],
        },
    });

    // ── JWT Authentication Middleware ──
    io.use(async (socket, next) => {
        const token = socket.handshake.auth.token;

        if (!token) {
            console.log('[SOCKET] ❌ Connection rejected — No token provided');
            return next(new Error('Authentication error: Token required'));
        }

        try {
            const decoded = jwt.verify(token, process.env.JWT_SECRET);

            // Spring Boot JWT puts email as 'sub' and role as 'role'
            const email = decoded.sub || decoded.email;
            const role = decoded.role || 'CUSTOMER';

            // Resolve numeric user ID from the email via DB lookup
            let userId = decoded.userId || decoded.id;
            if (!userId && email) {
                userId = await resolveUserId(email);
            }

            if (!userId) {
                console.log('[SOCKET] ❌ Connection rejected — Could not resolve user ID for:', email);
                return next(new Error('Authentication error: Could not resolve user'));
            }

            socket.userId = userId;
            socket.email = email;
            socket.role = role.toUpperCase();
            next();
        } catch (err) {
            console.log('[SOCKET] ❌ Connection rejected — Invalid token:', err.message);
            return next(new Error('Authentication error: Invalid token'));
        }
    });

    // ── Connection Handler ──
    io.on('connection', (socket) => {
        const { userId, email, role } = socket;

        // Join user-specific room
        const userRoom = `user_${userId}`;
        socket.join(userRoom);

        // Join role-based room
        const roleRoom = `role_${role}`;
        socket.join(roleRoom);

        console.log(`[SOCKET CONNECTED] 🟢 User ${userId} (${email}) | Role: ${role} | Rooms: ${userRoom}, ${roleRoom}`);

        // Handle disconnect
        socket.on('disconnect', (reason) => {
            console.log(`[SOCKET DISCONNECTED] 🔴 User ${userId} | Reason: ${reason}`);
        });

        // Handle errors
        socket.on('error', (err) => {
            console.error(`[SOCKET ERROR] ⚠️ User ${userId}:`, err.message);
        });
    });

    console.log('[SOCKET] ✅ Socket.IO server initialized');
    return io;
}

/**
 * Get the Socket.IO server instance
 * @returns {Server} Socket.IO instance
 */
function getIO() {
    if (!io) {
        throw new Error('Socket.IO not initialized. Call initializeSocket() first.');
    }
    return io;
}

/**
 * Emit a notification event to a specific user
 * @param {number} userId - Target user ID
 * @param {string} eventType - Event type name
 * @param {object} payload - Notification payload
 */
function emitToUser(userId, eventType, payload) {
    if (!io) return;
    const room = `user_${userId}`;
    io.to(room).emit('notification', payload);
    console.log(`[NOTIFICATION SENT] 📨 Event: ${eventType} → User: ${userId}`);
}

/**
 * Emit a notification event to all users with a specific role
 * @param {string} role - Target role (ADMIN, AGENT, CUSTOMER, CLAIMS_MANAGER)
 * @param {string} eventType - Event type name
 * @param {object} payload - Notification payload
 */
function emitToRole(role, eventType, payload) {
    if (!io) return;
    const room = `role_${role}`;
    io.to(room).emit('notification', payload);
    console.log(`[NOTIFICATION SENT] 📨 Event: ${eventType} → Role: ${role}`);
}

module.exports = { initializeSocket, getIO, emitToUser, emitToRole };

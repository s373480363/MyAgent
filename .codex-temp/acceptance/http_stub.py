from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('Content-Length', '0'))
        raw = self.rfile.read(length).decode('utf-8') if length else ''
        try:
            parsed = json.loads(raw) if raw else None
        except Exception:
            parsed = raw
        payload = {
            'ok': True,
            'summary': 'stub-ok',
            'received': parsed,
            'method': self.command,
            'path': self.path,
        }
        body = json.dumps(payload).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        return

HTTPServer(('127.0.0.1', 18081), Handler).serve_forever()

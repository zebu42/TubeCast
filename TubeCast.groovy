
set 'port', 4242

get("/") {
	def ua = headers['user-agent']
    "Your user-agent: ${ua}"
}

get("/foo/:bar") {
	"Hello, ${urlparams.bar}"
}

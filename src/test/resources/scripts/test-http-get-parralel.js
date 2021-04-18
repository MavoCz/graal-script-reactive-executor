function range(start, count) {
    return Array.apply(0, Array(count))
        .map((element, index) => index + start);
}

(async function fetch() {
    return Promise.all(
        range(1, 200)
            .map(num => client.get('/company/info')
            .then(response => JSON.parse(response.data))),
        )
})

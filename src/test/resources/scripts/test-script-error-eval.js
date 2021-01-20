(async function fetch() {
    //const company = await client.get(config.get("baseurl") + '/company/info');
    console.log(company.status);

    return {
        company: company.json()
    }
})

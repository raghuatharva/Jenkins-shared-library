def getAccountID(string environment) {
    switch (environment) {
        case 'dev':
            return '180294178330'
        case 'qa':
            return '180294178330'
        case 'uat':
            return '180294178330'
        case 'pre-prod':
            return '180294178330'
        case 'prod':
            return '180294178330'
        default:
            return 'nothing'
    }
}
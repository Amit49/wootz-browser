import { registerWallet } from './register.js';
import { WootzappWalletWallet } from './wallet.js';
export function initialize(wootzappWallet) {
    registerWallet(new WootzappWalletWallet(wootzappWallet));
}
//# sourceMappingURL=initialize.js.map
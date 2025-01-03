import { registerWallet } from './register.js';
import { WootzappWalletWallet } from './wallet.js';
import type { WootzappWallet } from './window.js';

export function initialize(wootzappWallet: WootzappWallet): void {
    registerWallet(new WootzappWalletWallet(wootzappWallet));
}

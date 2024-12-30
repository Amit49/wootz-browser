#!/usr/bin/env python

from __future__ import print_function
import json
import os
import sys

PLATFORM = {
    'cygwin': 'win32',
    'darwin': 'darwin',
    'linux2': 'linux',
    'linux': 'linux',
    'win32': 'win32',
}[sys.platform]

SOURCE_ROOT = os.path.abspath(
    os.path.join(os.path.dirname(__file__), '..', '..'))
CHROMIUM_ROOT = os.path.abspath(
    os.path.join(os.path.dirname(__file__), '..', '..', '..'))

verbose_mode = False


def dist_dir(target_os, target_arch):
    return os.path.join(output_dir(target_os, target_arch), 'dist')


def output_dir(target_os, target_arch):
    target_os_prefix = ''
    if target_os in ['android']:
        target_os_prefix = target_os + '_'

    target_arch_suffix = ''
    if target_arch != 'x64':
        target_arch_suffix = '_' + target_arch

    return os.path.join(CHROMIUM_ROOT, 'out', target_os_prefix + 'Release' + target_arch_suffix)


def enable_verbose_mode():
    print('Running in verbose mode')
    global verbose_mode
    verbose_mode = True


def is_verbose_mode():
    return verbose_mode

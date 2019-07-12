
# ####################################################################### #
# project: python-matsim
# setup.py
#                                                                         #
# ####################################################################### #
#                                                                         #
# copyright       : (C) 2019 by the members listed in the COPYING,        #
#                   LICENSE and WARRANTY file.                            #
#                                                                         #
# ####################################################################### #
#                                                                         #
#   This program is free software; you can redistribute it and/or modify  #
#   it under the terms of the GNU General Public License as published by  #
#   the Free Software Foundation; either version 2 of the License, or     #
#   (at your option) any later version.                                   #
#   See also COPYING, LICENSE and WARRANTY file                           #
#                                                                         #
# ####################################################################### #/

from setuptools import setup, find_packages, Command
from distutils.command.build import build

from buildutils.codegeneration import JavaAdapterCodeGenerationCommand


class MyBuild(build):
    sub_commands = [('codegen', None)] + build.sub_commands

with open('../README.md') as f:
    long_description = f.read()

setup(
    name='pythonmatsim',
    version='0.1a1',
    package_dir={'': 'generatedcode/'},
    # Note that this works only when code was already generated... Find a fix.
    packages=find_packages('generatedcode/', exclude=('buildutils', 'test')),
    #package_data = {
    #    #'buildutils': '*.xml',
    #    'javaresources': 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar',
    #},
    include_package_data=True,
    url='https://github.com/matsim-eth/python-matsim',
    license='GNU GPL 3.0',
    author='Thibaut Dubernet',
    author_email='thibaut.dubernet@ivt.baug.ethz.ch',
    description='A package to use MATSim from Python',
    long_description=long_description,
    long_description_content_type='text/markdown',
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Programming Language :: Java",
        "Programming Language :: Python :: 3.5",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "License :: OSI Approved :: GNU General Public License v3 or later (GPLv3+)",
        "Operating System :: OS Independent",
    ],
    setup_requires=[
        'numpy==1.16.3',
        'JPype1==0.7.0',
    ],
    install_requires=[
        'numpy==1.16.3',
        'JPype1==0.7.0',
        'protobuf==3.8.0',
    ],
    # Need type hints
    # Should also work with lower, but needs to be tested
    python_requires='>=3.5',
    cmdclass={
      'codegen': JavaAdapterCodeGenerationCommand,
      'build': MyBuild
    },
)

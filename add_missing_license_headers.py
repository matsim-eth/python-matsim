#!/bin/env python3

import pathlib
import tempfile
import shutil
import os

header = """
/* *********************************************************************** *
 * project: python-matsim
 * {filename}
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 """

test_header = '*   This program is free software; you can redistribute it and/or modify  *'

 
code_files = [f
              for ext in ('*.java', '*.py')
              for f in pathlib.Path('.').rglob(ext)]

for f in code_files:
    file_content = open(f, newline='').read()
    if not test_header in file_content:
        print(f'adding header to file {f}')
        with tempfile.NamedTemporaryFile('w') as writer:
            writer.write(header.format(filename=os.path.basename(f.name)))
            writer.write(file_content)
            writer.flush()

            try:
                shutil.copyfile(writer.name, f)
            except PermissionError:
                # Happens for files in venv. Quick and dirty way to get this through.
                pass

